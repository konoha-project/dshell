package dshell.lib;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import dshell.exception.DShellException;
import dshell.exception.MultipleException;
import dshell.exception.NullException;

import static dshell.lib.TaskOption.Behavior.printable ;
import static dshell.lib.TaskOption.Behavior.throwable ;
import static dshell.lib.TaskOption.Behavior.background;
import static dshell.lib.TaskOption.Behavior.receivable;

public class Task implements Serializable {
	private static final long serialVersionUID = 7531968866962967914L;
	transient private Thread stateMonitor;
	transient private TaskBuilder taskBuilder;
	transient private MessageStreamHandler stdoutHandler;
	transient private MessageStreamHandler stderrHandler;
	private boolean terminated = false;
	private final boolean isAsyncTask;
	private String stdoutMessage;
	private String stderrMessage;
	private ArrayList<Integer> exitStatusList;
	private String representString;
	private DShellException exception = new NullException("");

	public Task(TaskBuilder taskBuilder) {
		this.taskBuilder = taskBuilder;
		// start task
		TaskOption option = this.taskBuilder.getOption();
		PseudoProcess[] Processes = this.taskBuilder.getProcesses();
		int ProcessSize = Processes.length;
		Processes[0].start();
		for(int i = 1; i < ProcessSize; i++) {
			Processes[i].start();
			Processes[i].pipe(Processes[i - 1]);
		}
		// Start Message Handler
		// stdout
		this.stdoutHandler = this.createStdoutHandler();
		this.stdoutHandler.startHandler();
		// stderr
		this.stderrHandler = this.createStderrHandler();
		this.stderrHandler.startHandler();
		// start monitor
		this.isAsyncTask = option.is(background);
		StringBuilder sBuilder;
		sBuilder = new StringBuilder();
		if(this.isAsyncTask) {
			sBuilder.append("#AsyncTask");
		}
		else {
			sBuilder.append("#SyncTask");
		}
		sBuilder.append("\n");
		sBuilder.append(this.taskBuilder.toString());
		this.representString = sBuilder.toString();
		if(this.isAsyncTask) {
			this.stateMonitor = new Thread() {
				@Override public void run() {
					if(timeoutIfEnable()) {
						return;
					}
					while(true) {
						if(checkTermination()) {
							StringBuilder msgBuilder = new StringBuilder();
							msgBuilder.append("Terminated Task: " + representString);
							System.err.println(msgBuilder.toString());
							// run exit handler
							return;
						}
						try {
							Thread.sleep(100); // sleep thread
						}
						catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
				}
			};
			this.stateMonitor.start();
		}
	}

	private MessageStreamHandler createStdoutHandler() {
		TaskOption option = this.taskBuilder.getOption();
		if(option.supportStdoutHandler()) {
			OutputStream stdoutStream = null;
			if(option.is(printable)) {
				stdoutStream = System.out;
			}
			PseudoProcess[] procs = this.taskBuilder.getProcesses();
			PseudoProcess lastProc = procs[procs.length - 1];
			InputStream[] srcOutStreams = new InputStream[1];
			srcOutStreams[0] = lastProc.accessOutStream();
			return new MessageStreamHandler(srcOutStreams, stdoutStream);
		}
		return new EmptyMessageStreamHandler();
	}

	private MessageStreamHandler createStderrHandler() {
		TaskOption option = this.taskBuilder.getOption();
		if(option.supportStderrHandler()) {
			PseudoProcess[] procs = this.taskBuilder.getProcesses();
			int size = procs.length;
			InputStream[] srcErrorStreams = new InputStream[size];
			for(int i = 0; i < size; i++) {
				srcErrorStreams[i] = procs[i].accessErrorStream();
			}
			return new MessageStreamHandler(srcErrorStreams, System.err);
		}
		return new EmptyMessageStreamHandler();
	}

	@Override public String toString() {
		return this.representString;
	}

	private void joinAndSetException() {
		this.terminated = true;
		if(!this.isAsyncTask) {
			if(!this.timeoutIfEnable()) {
				this.waitTermination();
			}
		}
		else {
			try {
				stateMonitor.join();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		this.stdoutMessage = this.stdoutHandler.waitTermination();
		this.stderrMessage = this.stderrHandler.waitTermination();
		this.exitStatusList = new ArrayList<Integer>();
		for(PseudoProcess proc : this.taskBuilder.getProcesses()) {
			this.exitStatusList.add(proc.getRet());
		}
		// exception raising
		this.exception = new ShellExceptionBuilder(this.taskBuilder, this.stderrHandler.getEachBuffers()).getException();
	}

	public void join() {
		if(this.terminated) {
			return;
		}
		this.joinAndSetException();
		TaskOption option = this.taskBuilder.getOption();
		if(!option.is(receivable) && option.is(throwable) && !(this.exception instanceof NullException)) {
			throw this.exception;
		}
	}

	public void join(long timeout) {
		
	}

	public String getOutMessage() {
		this.join();
		return this.stdoutMessage;
	}

	public String getErrorMessage() {
		this.join();
		return this.stderrMessage;
	}

	public int getExitStatus() {
		this.join();
		return this.exitStatusList.get(this.exitStatusList.size() - 1);
	}

	private boolean timeoutIfEnable() {
		long timeout = this.taskBuilder.getTimeout();
		if(timeout > 0) { // timeout
			try {
				Thread.sleep(timeout);	// ms
				StringBuilder msgBuilder = new StringBuilder();
				msgBuilder.append("Timeout Task: " + this.toString());
				this.kill();
				System.err.println(msgBuilder.toString());
				// run exit handler
				return true;
			} 
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return false;
	}

	private void kill() {
		PseudoProcess[] procs = this.taskBuilder.getProcesses();
		for(int i = 0; i < procs.length; i++) {
			procs[i].kill();
		}
	}

	private void waitTermination() {
		PseudoProcess[] procs = this.taskBuilder.getProcesses();
		for(int i = 0; i < procs.length; i++) {
			procs[i].waitTermination();
		}
	}

	private boolean checkTermination() {
		PseudoProcess[] procs = this.taskBuilder.getProcesses();
		for(int i = 0; i < procs.length; i++) {
			if(!procs[i].checkTermination()) {
				return false;
			}
		}
		return true;
	}
}

class MessageStreamHandler {
	private InputStream[] srcStreams;
	private OutputStream consoleStream;
	private ByteArrayOutputStream messageBuffer;
	private ByteArrayOutputStream[] eachBuffers;
	private PipeStreamHandler[] streamHandlers;

	public MessageStreamHandler() {	// do nothing
	}

	public MessageStreamHandler(InputStream[] srcStreams, OutputStream consoleStream) {
		this.srcStreams = srcStreams;
		this.messageBuffer = new ByteArrayOutputStream();
		this.streamHandlers = new PipeStreamHandler[this.srcStreams.length];
		this.consoleStream = consoleStream;
		this.eachBuffers = new ByteArrayOutputStream[this.srcStreams.length];
	}

	public void startHandler() {
		boolean[] closeOutputs = {false, false, false};
		for(int i = 0; i < srcStreams.length; i++) {
			this.eachBuffers[i] = new ByteArrayOutputStream();
			OutputStream[] destStreams = new OutputStream[]{this.consoleStream, this.messageBuffer, this.eachBuffers[i]};
			this.streamHandlers[i] = new PipeStreamHandler(this.srcStreams[i], destStreams, true, closeOutputs);
			this.streamHandlers[i].start();
		}
	}

	public String waitTermination() {
		for(int i = 0; i < srcStreams.length; i++) {
			try {
				this.streamHandlers[i].join();
			}
			catch(InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return this.messageBuffer.toString();
	}

	public ByteArrayOutputStream[] getEachBuffers() {
		return this.eachBuffers;
	}
}

class EmptyMessageStreamHandler extends MessageStreamHandler {
	@Override
	public void startHandler() { // do nothing
	}

	@Override
	public String waitTermination() {
		return "";
	}

	@Override
	public ByteArrayOutputStream[] getEachBuffers() {
		return new ByteArrayOutputStream[0];
	}
}

class ShellExceptionBuilder {
	private TaskBuilder taskBuilder;
	private final CauseInferencer inferencer;
	private final ByteArrayOutputStream[] eachBuffers;

	public ShellExceptionBuilder(TaskBuilder taskBuilder, ByteArrayOutputStream[] eachBuffers) {
		this.taskBuilder = taskBuilder;
		this.inferencer = new CauseInferencer_ltrace();
		this.eachBuffers = eachBuffers;
	}

	public DShellException getException() {
		PseudoProcess[] procs = taskBuilder.getProcesses();
		boolean enableException = this.taskBuilder.getOption().is(throwable);
		if(!enableException || taskBuilder.getTimeout() > 0) {
			return new NullException("");
		}
		ArrayList<DShellException> exceptionList = new ArrayList<DShellException>();
		for(int i = 0; i < procs.length; i++) {
			PseudoProcess proc = procs[i];
			String errorMessage = this.eachBuffers[i].toString();
			this.createAndAddException(exceptionList, proc, errorMessage);
		}
		int size = exceptionList.size();
		if(size == 1) {
			if(!(exceptionList.get(0) instanceof NullException)) {
				return exceptionList.get(0);
			}
		}
		else if(size > 1) {
			int count = 0;
			for(DShellException exception : exceptionList) {
				if(!(exception instanceof NullException)) {
					count++;
				}
			}
			if(count != 0) {
				return new MultipleException("", exceptionList.toArray(new DShellException[size]));
			}
		}
		return new NullException("");
	}

	private void createAndAddException(ArrayList<DShellException> exceptionList, PseudoProcess proc, String errorMessage) {
		String message = proc.getCmdName();
		if(proc.isTraced() || proc.getRet() != 0) {
			DShellException exception;
			if(proc.isTraced()) {
				ArrayList<String> infoList = this.inferencer.doInference((SubProc)proc);
				exception = ExceptionClassMap.createException(message, infoList.toArray(new String[infoList.size()]));
			}
			else {
				exception = new DShellException(message);
			}
			exception.setErrorMessage(errorMessage);
			exceptionList.add(exception);
		}
		else {
			exceptionList.add(new NullException(message));
		}
		if(proc instanceof SubProc) {
			((SubProc)proc).deleteLogFile();
		}
	}
}

