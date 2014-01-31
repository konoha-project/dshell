package dshell.lib;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import dshell.exception.DShellException;
import dshell.exception.MultipleException;

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
		this.stdoutHandler.showMessage();
		// stderr
		this.stderrHandler = this.createStderrHandler();
		this.stderrHandler.showMessage();
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
		new ShellExceptionRaiser(this.taskBuilder).raiseException();
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
	private OutputStream[] destStreams;
	private ByteArrayOutputStream messageBuffer;
	private PipeStreamHandler[] streamHandlers;

	public MessageStreamHandler() {	// do nothing
	}

	public MessageStreamHandler(InputStream[] srcStreams, OutputStream destStream) {
		this.srcStreams = srcStreams;
		this.messageBuffer = new ByteArrayOutputStream();
		this.streamHandlers = new PipeStreamHandler[this.srcStreams.length];
		this.destStreams = new OutputStream[]{destStream, this.messageBuffer};
	}

	public void showMessage() {
		boolean[] closeOutputs = {false, false};
		for(int i = 0; i < srcStreams.length; i++) {
			this.streamHandlers[i] = new PipeStreamHandler(this.srcStreams[i], this.destStreams, true, closeOutputs);
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
}

class EmptyMessageStreamHandler extends MessageStreamHandler {
	@Override
	public void showMessage() { // do nothing
	}

	@Override
	public String waitTermination() {
		return "";
	}
}

class ShellExceptionRaiser {
	private TaskBuilder taskBuilder;
	private final CauseInferencer inferencer;

	public ShellExceptionRaiser(TaskBuilder taskBuilder) {
		this.taskBuilder = taskBuilder;
		this.inferencer = new CauseInferencer_ltrace();
	}

	public void raiseException() {
		PseudoProcess[] procs = taskBuilder.getProcesses();
		boolean enableException = this.taskBuilder.getOption().is(throwable);
		if(!enableException || taskBuilder.getTimeout() > 0) {
			return;
		}
		ArrayList<RuntimeException> exceptionList = new ArrayList<RuntimeException>();
		for(PseudoProcess proc : procs) {
			this.createAndAddException(exceptionList, proc);
		}
		int size = exceptionList.size();
		if(size == 1) {	//TODO: MultipleException
			if(exceptionList.get(0) != null) {
				throw exceptionList.get(0);
			}
		}
		else if(size > 1) {
			throw new MultipleException("", exceptionList.toArray(new DShellException[size]));
		}
	}

	private void createAndAddException(ArrayList<RuntimeException> exceptionList, PseudoProcess proc) {
		if(proc.isTraced() || proc.getRet() != 0) {
			String message = proc.getCmdName();
			if(proc.isTraced()) {
				ArrayList<String> infoList = this.inferencer.doInference((SubProc)proc);
				exceptionList.add(ExceptionClassMap.createException(message, infoList.toArray(new String[infoList.size()])));
			}
			else {
				exceptionList.add(new DShellException(message));
			}
		}
		if(proc instanceof SubProc) {
			((SubProc)proc).deleteLogFile();
		}
	}
}

