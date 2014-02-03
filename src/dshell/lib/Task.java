package dshell.lib;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import dshell.exception.DShellException;
import dshell.exception.MultipleException;
import dshell.exception.NullException;
import static dshell.lib.TaskOption.Behavior.printable ;
import static dshell.lib.TaskOption.Behavior.throwable ;
import static dshell.lib.TaskOption.Behavior.background;

public class Task {
	private ProcMonitor monitor;
	private TaskBuilder taskBuilder;
	private boolean terminated = false;
	private final boolean isAsyncTask;
	private MessageStreamHandler stdoutHandler;
	private MessageStreamHandler stderrHandler;
	private String stdoutMessage;
	private String stderrMessage;
	private StringBuilder sBuilder;

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
		this.sBuilder = new StringBuilder();
		if(this.isAsyncTask) {
			this.sBuilder.append("#AsyncTask");
		}
		else {
			this.sBuilder.append("#SyncTask");
		}
		this.sBuilder.append("\n");
		this.sBuilder.append(this.taskBuilder.toString());
		this.monitor = new ProcMonitor(this, this.taskBuilder, isAsyncTask);
		this.monitor.start();
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
		return this.sBuilder.toString();
	}

	public void join() {
		if(this.terminated) {
			return;
		}
		try {
			this.terminated = true;
			this.stdoutMessage = this.stdoutHandler.waitTermination();
			this.stderrMessage = this.stderrHandler.waitTermination();
			monitor.join();
		} 
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		new ShellExceptionRaiser(this.taskBuilder, this.stderrHandler.getEachBuffers()).raiseException();
	}

	public void join(long timeout) {
		
	}

	public String getOutMessage() {
		this.checkTermination();
		return this.stdoutMessage;
	}

	public String getErrorMessage() {
		this.checkTermination();
		return this.stderrMessage;
	}

	public int getExitStatus() {
		this.checkTermination();
		PseudoProcess[] procs = this.taskBuilder.getProcesses();
		return procs[procs.length - 1].getRet();
	}

	private void checkTermination() {
		if(!this.terminated) {
			throw new IllegalThreadStateException("Task is not Terminated");
		}
	}
}

class ProcMonitor extends Thread {	// TODO: support exit handle
	private Task task;
	private TaskBuilder taskBuilder;
	private final boolean isBackground;
	public final long timeout;

	public ProcMonitor(Task task, TaskBuilder taskBuilder, boolean isBackground) {
		this.task = task;
		this.taskBuilder = taskBuilder;
		this.isBackground = isBackground;
		this.timeout =  this.taskBuilder.getTimeout();
	}

	@Override public void run() {
		PseudoProcess[] processes = this.taskBuilder.getProcesses();
		int size = processes.length;
		if(this.timeout > 0) { // timeout
			try {
				Thread.sleep(timeout);	// ms
				StringBuilder msgBuilder = new StringBuilder();
				msgBuilder.append("Timeout Task: ");
				for(int i = 0; i < size; i++) {
					processes[i].kill();
					if(i != 0) {
						msgBuilder.append("| ");
					}
					msgBuilder.append(processes[i].getCmdName());
				}
				System.err.println(msgBuilder.toString());
				// run exit handler
			} 
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return;
		}
		if(!this.isBackground) {	// wait termination for sync task
			for(int i = 0; i < size; i++) {
				processes[i].waitTermination();
			}
		}
		while(this.isBackground) {	// check termination for async task
			int count = 0;
			for(int i = 0; i < size; i++) {
				SubProc subProc = (SubProc)processes[i];
				try {
					subProc.checkTermination();
					count++;
				}
				catch(IllegalThreadStateException e) {
					// process has not terminated yet. do nothing
				}
			}
			if(count == size) {
				StringBuilder msgBuilder = new StringBuilder();
				msgBuilder.append("Terminated Task: ");
				for(int i = 0; i < size; i++) {
					if(i != 0) {
						msgBuilder.append("| ");
					}
					msgBuilder.append(processes[i].getCmdName());
				}
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
}

class MessageStreamHandler {
	private InputStream[] srcStreams;
	private OutputStream destStream;
	private ByteArrayOutputStream messageBuffer;
	private ByteArrayOutputStream[] eachBuffers;
	private PipeStreamHandler[] streamHandlers;

	public MessageStreamHandler() {	// do nothing
	}

	public MessageStreamHandler(InputStream[] srcStreams, OutputStream destStream) {
		this.srcStreams = srcStreams;
		this.messageBuffer = new ByteArrayOutputStream();
		this.streamHandlers = new PipeStreamHandler[this.srcStreams.length];
		this.destStream = destStream;
		this.eachBuffers = new ByteArrayOutputStream[this.srcStreams.length];
	}

	public void startHandler() {
		boolean[] closeOutputs = {false, false, false};
		for(int i = 0; i < srcStreams.length; i++) {
			this.eachBuffers[i] = new ByteArrayOutputStream();
			OutputStream[] destStreams = new OutputStream[]{destStream, this.messageBuffer, this.eachBuffers[i]};
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

class ShellExceptionRaiser {
	private TaskBuilder taskBuilder;
	private final CauseInferencer inferencer;
	private final ByteArrayOutputStream[] eachBuifers;

	public ShellExceptionRaiser(TaskBuilder taskBuilder, ByteArrayOutputStream[] eachBuifers) {
		this.taskBuilder = taskBuilder;
		this.inferencer = new CauseInferencer_ltrace();
		this.eachBuifers = eachBuifers;
	}

	public void raiseException() {
		PseudoProcess[] procs = taskBuilder.getProcesses();
		boolean enableException = this.taskBuilder.getOption().is(throwable);
		if(!enableException || taskBuilder.getTimeout() > 0) {
			return;
		}
		ArrayList<DShellException> exceptionList = new ArrayList<DShellException>();
		for(int i = 0; i < procs.length; i++) {
			PseudoProcess proc = procs[i];
			String errorMessage = this.eachBuifers[i].toString();
			this.createAndAddException(exceptionList, proc, errorMessage);
		}
		int size = exceptionList.size();
		if(size == 1) {
			if(!(exceptionList.get(0) instanceof NullException)) {
				throw exceptionList.get(0);
			}
		}
		else if(size > 1) {
			int count = 0;
			for(DShellException exception : exceptionList) {
				if(!(exception instanceof NullException)) {
					count++;
				}
			}
			if(count != size) {
				throw new MultipleException("", exceptionList.toArray(new DShellException[size]));
			}
		}
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

