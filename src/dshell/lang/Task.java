package dshell.lang;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import dshell.internal.lib.Utils;
import dshell.internal.process.AbstractProcessContext;
import dshell.internal.process.ShellExceptionBuilder;
import dshell.internal.process.TaskOption;
import dshell.internal.process.PipeStreamHandler.EmptyMessageStreamHandler;
import dshell.internal.process.PipeStreamHandler.MessageStreamHandler;
import dshell.internal.process.PipeStreamHandler.MessageStreamHandlerOp;
import static dshell.internal.process.TaskOption.Behavior.background;
import static dshell.internal.process.TaskOption.Behavior.printable;
import static dshell.internal.process.TaskOption.Behavior.receiver;
import static dshell.internal.process.TaskOption.Behavior.throwable;

public class Task implements Serializable {
	private static final long serialVersionUID = 7531968866962967914L;

	transient private Thread stateMonitor;
	transient private final List<AbstractProcessContext> procContexts;
	transient private final TaskOption option;
	transient private MessageStreamHandlerOp stdoutHandler;
	transient private MessageStreamHandlerOp stderrHandler;
	transient private List<Task> taskList;

	private boolean terminated = false;
	private String stdoutMessage;
	private String stderrMessage;
	private List<Integer> exitStatusList;
//	private final String representString;
	private DShellException exception = DShellException.createNullException("");

	public Task(List<AbstractProcessContext> procContexts, TaskOption option) {
		this.procContexts = procContexts;
		this.option = option;
		// start task
		int size = this.procContexts.size();
		this.procContexts.get(0).start();
		for(int i = 1; i < size; i++) {
			this.procContexts.get(i).start().pipe(this.procContexts.get(i - 1));
		}
		// start message handler
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
						System.err.println("Terminated Task: " + "FIXME");
						// run exit handler
						return;
					}
					try {
						Thread.sleep(100); // sleep thread
					} catch (InterruptedException e) {
						System.err.println(e.getMessage());
						Utils.fatal(1, "interrupt problem");
					}
				}
			}
		};
		this.stateMonitor.start();
	}

	private MessageStreamHandlerOp createStdoutHandler() {
		if(!this.option.supportStdoutHandler()) {
			return EmptyMessageStreamHandler.getHandler();
		}
		OutputStream stdoutStream = null;
		if(this.option.is(printable)) {
			stdoutStream = System.out;
		}
		AbstractProcessContext LastContext = this.procContexts.get(this.procContexts.size() - 1);
		InputStream[] srcOutStreams = new InputStream[] {LastContext.accessOutStream()};
		return new MessageStreamHandler(srcOutStreams, stdoutStream);
	}

	private MessageStreamHandlerOp createStderrHandler() {
		if(!this.option.supportStderrHandler()) {
			return EmptyMessageStreamHandler.getHandler();
		}
		int size = this.procContexts.size();
		InputStream[] srcErrorStreams = new InputStream[size];
		for(int i = 0; i < size; i++) {
			srcErrorStreams[i] = this.procContexts.get(i).accessErrorStream();
		}
		return new MessageStreamHandler(srcErrorStreams, System.err);
	}

	@Override public String toString() {
		//return this.representString;
		return "FIXME";
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
		for(AbstractProcessContext proc : this.procContexts) {
			this.exitStatusList.add(proc.getRet());
		}
		// exception raising
		this.exception = ShellExceptionBuilder.getException(this.procContexts, this.option, this.stderrHandler.getEachBuffers());
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
		for(AbstractProcessContext proc : this.procContexts) {
			proc.kill();
		}
	}

	private void waitTermination() {
		for(AbstractProcessContext proc : this.procContexts) {
			proc.waitTermination();
		}
	}

	private boolean checkTermination() {
		for(AbstractProcessContext proc : this.procContexts) {
			if(proc.checkTermination()) {
				return false;
			}
		}
		return true;
	}

	private void getRemoteTaskResult() {	//TODO:
//		if(!this.option.is(sender) && !(this.procs[this.procs.length - 1] instanceof RequestSender)) {
//			return;
//		}
//		RequestSender sender = (RequestSender) this.procs[this.procs.length - 1];
//		Task remoteTask = sender.getRemoteTask();
//		if(remoteTask != null) {
//			this.stdoutMessage = remoteTask.getOutMessage();
//			this.stderrMessage = remoteTask.getErrorMessage();
//			this.exception = remoteTask.exception;
//			if(this.option.is(printable)) {
//				System.out.println(this.stdoutMessage);
//			}
//		}
	}

	public static GenericArray getTaskArray(Task task) {	//TODO:
		Task[] values = new Task[task.taskList.size()];
		for(int i = 0; i < values.length; i++) {
			values[i] = task.taskList.get(i);
		}
		return new GenericArray(values);
	}
}
