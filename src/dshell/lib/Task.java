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
import static dshell.lib.TaskOption.Behavior.timeout;

public class Task implements Serializable {
	private static final long serialVersionUID = 7531968866962967914L;
	transient private Thread stateMonitor;
	transient private final PseudoProcess[] procs;
	transient private final TaskOption option;
	transient private MessageStreamHandler stdoutHandler;
	transient private MessageStreamHandler stderrHandler;
	private boolean terminated = false;
	private String stdoutMessage;
	private String stderrMessage;
	private ArrayList<Integer> exitStatusList;
	private final String representString;
	private DShellException exception = new NullException("");

	public Task(PseudoProcess[] procs, TaskOption option, String represent) {
		this.option = option;
		this.procs = procs;
		this.representString = represent;
		// start task
		int ProcessSize = this.procs.length;
		this.procs[0].start();
		for(int i = 1; i < ProcessSize; i++) {
			this.procs[i].start();
			this.procs[i].pipe(this.procs[i - 1]);
		}
		// Start Message Handler
		// stdout
		this.stdoutHandler = this.createStdoutHandler();
		this.stdoutHandler.startHandler();
		// stderr
		this.stderrHandler = this.createStderrHandler();
		this.stderrHandler.startHandler();
		// start state monitor
		if(option.is(background)) {
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
		if(this.option.supportStdoutHandler()) {
			OutputStream stdoutStream = null;
			if(this.option.is(printable)) {
				stdoutStream = System.out;
			}
			PseudoProcess lastProc = this.procs[this.procs.length - 1];
			InputStream[] srcOutStreams = new InputStream[1];
			srcOutStreams[0] = lastProc.accessOutStream();
			return new MessageStreamHandler(srcOutStreams, stdoutStream);
		}
		return new EmptyMessageStreamHandler();
	}

	private MessageStreamHandler createStderrHandler() {
		if(this.option.supportStderrHandler()) {
			int size = this.procs.length;
			InputStream[] srcErrorStreams = new InputStream[size];
			for(int i = 0; i < size; i++) {
				srcErrorStreams[i] = this.procs[i].accessErrorStream();
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
		if(!option.is(background)) {
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
		for(PseudoProcess proc : this.procs) {
			this.exitStatusList.add(proc.getRet());
		}
		// exception raising
		this.exception = ShellExceptionBuilder.getException(this.procs, this.option, this.stderrHandler.getEachBuffers());
	}

	public void join() {
		if(this.terminated) {
			return;
		}
		this.joinAndSetException();
		if(!this.option.is(receivable) && this.option.is(throwable) && !(this.exception instanceof NullException)) {
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
		long timeout = this.option.getTimeout();
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
		for(PseudoProcess proc : this.procs) {
			proc.kill();
		}
	}

	private void waitTermination() {
		for(PseudoProcess proc : this.procs) {
			proc.waitTermination();
		}
	}

	private boolean checkTermination() {
		for(PseudoProcess proc : this.procs) {
			if(proc.checkTermination()) {
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
		for(PipeStreamHandler streamHandler : this.streamHandlers) {
			try {
				streamHandler.join();
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
	public static DShellException getException(final PseudoProcess[] procs, final TaskOption option, final ByteArrayOutputStream[] eachBuffers) {
		if(!option.is(throwable) || option.is(timeout)) {
			return new NullException("");
		}
		ArrayList<DShellException> exceptionList = new ArrayList<DShellException>();
		for(int i = 0; i < procs.length; i++) {
			PseudoProcess proc = procs[i];
			String errorMessage = eachBuffers[i].toString();
			createAndAddException(exceptionList, proc, errorMessage);
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

	private static void createAndAddException(ArrayList<DShellException> exceptionList, PseudoProcess proc, String errorMessage) {
		CauseInferencer inferencer = CauseInferencer_ltrace.getInferencer();
		String message = proc.getCmdName();
		if(proc.isTraced() || proc.getRet() != 0) {
			DShellException exception;
			if(proc.isTraced()) {
				ArrayList<String> infoList = inferencer.doInference((SubProc)proc);
				exception = ExceptionClassMap.createException(message, infoList.toArray(new String[infoList.size()]));
			}
			else {
				exception = new DShellException(message);
			}
			exception.setCommand(message);
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

