package dshell.internal.process;

import static dshell.internal.process.TaskOption.Behavior.background;
import static dshell.internal.process.TaskOption.Behavior.printable;
import static dshell.internal.process.TaskOption.Behavior.returnable;
import static dshell.internal.process.TaskOption.Behavior.throwable;
import static dshell.internal.process.TaskOption.RetType.IntType;
import static dshell.internal.process.TaskOption.RetType.StringType;
import static dshell.internal.process.TaskOption.RetType.TaskArrayType;
import static dshell.internal.process.TaskOption.RetType.TaskType;
import static dshell.internal.process.TaskOption.RetType.VoidType;

import java.util.ArrayList;
import java.util.List;

import dshell.internal.lib.RuntimeContext;
import dshell.internal.lib.Utils;
import dshell.lang.GenericArray;
import dshell.lang.Task;

public class TaskContext {
	private final List<AbstractProcessContext> procContexts;
	private final TaskOption option;
	private boolean enableTrace = false;

	public TaskContext() {
		this.procContexts = new ArrayList<>();
		this.option = new TaskOption();
	}

	public TaskContext addContext(AbstractProcessContext context) {
		this.procContexts.add(context);
		if(context.isTraced()) {
			this.enableTrace = true;
		}
		if(context instanceof ProcessContext) {
			((ProcessContext) context).setStreamBehavior(this.option);
		}
		return this;
	}

	public static AbstractProcessContext createProcessContext(String commandName) {
		if(commandName.indexOf('/') == -1) {
			return RuntimeContext.getInstance().getBuiltinCommand(commandName);
		}
		return new ProcessContext(commandName);
	}

	private static boolean checkTraceRequirements() {
		if(System.getProperty("os.name").equals("Linux")) {
			ProcessContext.traceBackendType = ProcessContext.traceBackend_ltrace;
			return Utils.getCommandFromPath("ltrace") != null;
		}
		return false;
	}

	private Object execTask() {
		/**
		 * init system call trace.
		 */
		if(this.enableTrace) {
			boolean tracable = checkTraceRequirements();
			for(AbstractProcessContext context : this.procContexts) {
				if(context instanceof ProcessContext) {
					((ProcessContext) context).initTrace(tracable);
				}
			}
			if(!tracable) {
				System.err.println("Systemcall Trace is Not Supported");
			}
		}

		/**
		 * launch task.
		 */
		Task task = new Task(this.procContexts, this.option);
		if(this.option.is(background)) {
			return (this.option.isRetType(TaskType) && this.option.is(returnable)) ? task : null;
		}
		task.join();
		if(this.option.is(returnable)) {
			if(this.option.isRetType(StringType)) {
				return task.getOutMessage();
			} else if(this.option.isRetType(IntType)) {
				return new Integer(task.getExitStatus());
			} else if(this.option.isRetType(TaskType)) {
				return task;
			} else if(this.option.isRetType(TaskArrayType)) {	//FIXME:
				return Task.getTaskArray(task);
			}
		}
		return null;
	}

	// launch task.
	public void execAsVoid() {
		this.option.setRetType(VoidType).setFlag(printable, true).setFlag(throwable, true);
		this.execTask();
	}

	public long execAsInt() {
		this.option.setRetType(IntType).setFlag(printable, true).setFlag(returnable, true);
		return ((Integer)this.execTask()).longValue();
	}

	public boolean execAsBoolean() {
		return this.execAsInt() == 0;
	}

	public String execAsString() {
		this.option.setRetType(StringType).setFlag(returnable, true);
		return (String) this.execTask();
	}

	public GenericArray execAsStringArray() {
		return new GenericArray(Utils.splitWithDelim(this.execAsString()));
	}

	public Task execAsTask() {
		this.option.setRetType(TaskType).
		setFlag(printable, true).setFlag(returnable, true).setFlag(throwable, true);
		return (Task) this.execTask();
	}
}
