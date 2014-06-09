package dshell.lang;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import dshell.annotation.TypeParameter;
import dshell.internal.lib.Utils;
import dshell.internal.process.PseudoProcess;
import dshell.internal.process.ShellExceptionBuilder;
import dshell.internal.process.TaskOption;
import dshell.internal.process.PipeStreamHandler.EmptyMessageStreamHandler;
import dshell.internal.process.PipeStreamHandler.MessageStreamHandler;
import dshell.internal.process.PipeStreamHandler.MessageStreamHandlerOp;
import dshell.internal.remote.RequestSender;
import static dshell.internal.process.TaskOption.Behavior.background;
import static dshell.internal.process.TaskOption.Behavior.printable;
import static dshell.internal.process.TaskOption.Behavior.receiver;
import static dshell.internal.process.TaskOption.Behavior.sender;
import static dshell.internal.process.TaskOption.Behavior.throwable;

public class Task implements Serializable {
	private static final long serialVersionUID = 7531968866962967914L;

	transient private Thread stateMonitor;
	transient private final PseudoProcess[] procs;
	transient private final TaskOption option;
	transient private MessageStreamHandlerOp stdoutHandler;
	transient private MessageStreamHandlerOp stderrHandler;
	transient private ArrayList<Task> taskList;

	private boolean terminated = false;
	private String stdoutMessage;
	private String stderrMessage;
	private ArrayList<Integer> exitStatusList;
	private final String representString;
	private DShellException exception = DShellException.createNullException("");

	public Task(PseudoProcess[] procs, TaskOption option, String represent) {
		this.option = option;
		this.procs = procs;
		this.representString = represent;
		// start task
		int procSize = this.procs.length;
		this.procs[0].start();
		for(int i = 1; i < procSize; i++) {
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
		if(!option.is(background)) {
			return;
		}
		this.stateMonitor = new Thread() {
			@Override public void run() {
				if(timeoutIfEnable()) {
					return;
				}
				while(true) {
					if(checkTermination()) {
						System.err.println("Terminated Task: " + representString);
						// run exit handler
						return;
					}
					try {
						Thread.sleep(100); // sleep thread
					}
					catch (InterruptedException e) {
						e.printStackTrace();
						Utils.fatal(1, "interrupt problem");
					}
				}
			}
		};
		this.stateMonitor.start();
	}

	private MessageStreamHandlerOp createStdoutHandler() {
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
		return EmptyMessageStreamHandler.getHandler();
	}

	private MessageStreamHandlerOp createStderrHandler() {
		if(this.option.supportStderrHandler()) {
			int size = this.procs.length;
			InputStream[] srcErrorStreams = new InputStream[size];
			for(int i = 0; i < size; i++) {
				srcErrorStreams[i] = this.procs[i].accessErrorStream();
			}
			return new MessageStreamHandler(srcErrorStreams, System.err);
		}
		return EmptyMessageStreamHandler.getHandler();
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
		// get remote task result if supported
		this.getRemoteTaskResult();
	}

	public void join() {
		if(this.terminated) {
			return;
		}
		this.joinAndSetException();
		if(!this.option.is(receiver) && this.option.is(throwable) && !(this.exception instanceof DShellException.NullException)) {
			throw this.exception;
		}
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
				e.printStackTrace();
				Utils.fatal(1, "interrupt problem");
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

	private void getRemoteTaskResult() {
		if(!this.option.is(sender) && !(this.procs[this.procs.length - 1] instanceof RequestSender)) {
			return;
		}
		RequestSender sender = (RequestSender) this.procs[this.procs.length - 1];
		Task remoteTask = sender.getRemoteTask();
		if(remoteTask != null) {
			this.stdoutMessage = remoteTask.getOutMessage();
			this.stderrMessage = remoteTask.getErrorMessage();
			this.exception = remoteTask.exception;
			if(this.option.is(printable)) {
				System.out.println(this.stdoutMessage);
			}
		}
	}

	@TypeParameter("Task")
	public static GenericArray getTaskArray(Task task) {	//TODO:
		Task[] values = new Task[task.taskList.size()];
		for(int i = 0; i < values.length; i++) {
			values[i] = task.taskList.get(i);
		}
		return new GenericArray(values);
	}
}
