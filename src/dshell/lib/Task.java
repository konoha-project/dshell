package dshell.lib;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import dshell.exception.DShellException;
import dshell.util.Utils;

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
		int OptionFlag = this.taskBuilder.getOptionFlag();
		PseudoProcess[] Processes = this.taskBuilder.getProcesses();
		int ProcessSize = Processes.length;
		int lastIndex = ProcessSize - 1;
		PseudoProcess lastProc = Processes[lastIndex];

		OutputStream stdoutStream = null;
		if(Utils.is(OptionFlag, Utils.printable)) {
			stdoutStream = System.out;
		}
		InputStream[] srcOutStreams = new InputStream[1];
		InputStream[] srcErrorStreams = new InputStream[ProcessSize];

		Processes[0].start();
		for(int i = 1; i < ProcessSize; i++) {
			Processes[i].start();
			Processes[i].pipe(Processes[i - 1]);
		}

		// Start Message Handler
		// stdout
		srcOutStreams[0] = lastProc.accessOutStream();
		this.stdoutHandler = new MessageStreamHandler(srcOutStreams, stdoutStream);
		this.stdoutHandler.showMessage();
		// stderr
		for(int i = 0; i < ProcessSize; i++) {
			srcErrorStreams[i] = Processes[i].accessErrorStream();
		}
		this.stderrHandler = new MessageStreamHandler(srcErrorStreams, System.err);
		this.stderrHandler.showMessage();
		// start monitor
		this.isAsyncTask = Utils.is(this.taskBuilder.getOptionFlag(), Utils.background);
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

class ShellExceptionRaiser {
	private TaskBuilder taskBuilder;
	private final CauseInferencer inferencer;

	public ShellExceptionRaiser(TaskBuilder taskBuilder) {
		this.taskBuilder = taskBuilder;
		this.inferencer = new CauseInferencer_ltrace();
	}

	public void raiseException() {
		PseudoProcess[] procs = taskBuilder.getProcesses();
		boolean enableException = Utils.is(this.taskBuilder.getOptionFlag(), Utils.throwable);
		if(!enableException || taskBuilder.getTimeout() > 0) {
			return;
		}
		ArrayList<RuntimeException> exceptionList = new ArrayList<RuntimeException>();
		for(PseudoProcess proc : procs) {
			this.createAndAddException(exceptionList, proc);
		}
		if(exceptionList.size() > 0) {	//TODO: MultipleException
			if(exceptionList.get(0) != null) {
				throw exceptionList.get(0);
			}
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

