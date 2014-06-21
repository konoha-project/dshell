package dshell.internal.process;

import static dshell.internal.process.TaskOption.Behavior.returnable;
import static dshell.internal.process.TaskOption.Behavior.sender;
import static dshell.internal.process.TaskOption.Behavior.throwable;
import static dshell.internal.process.TaskOption.Behavior.timeout;
import static dshell.internal.process.TaskOption.RetType.StringType;
import static dshell.internal.process.TaskOption.RetType.TaskType;

import java.io.Serializable;
import java.util.EnumSet;

/**
 * represent task option.
 * @author skgchxngsxyz-opensuse
 *
 */
public class TaskOption implements Serializable {
	private static final long serialVersionUID = 5651190312973095075L;

	public static enum Behavior {
		returnable,
		printable ,
		throwable ,
		background,
		sender,
		receiver,
		timeout   ,
	}

	public static enum RetType {
		VoidType   ,
		IntType    ,
		StringType ,
		TaskType   ,
		TaskArrayType,
	}

	/**
	 * return type of task.
	 */
	private RetType retType;

	private final EnumSet<Behavior> flagSet;
	private long time = -1;

	public TaskOption() {
		this.retType = RetType.IntType;
		this.flagSet = EnumSet.noneOf(Behavior.class);
	}

	public boolean isRetType(RetType type) {
		return this.retType == type;
	}

	public boolean is(Behavior optionFlag) {
		return this.flagSet.contains(optionFlag);
	}

	public TaskOption setRetType(RetType type) {
		this.retType = type;
		return this;
	}

	public TaskOption setFlag(Behavior optionFlag, boolean set) {
		if(set) {
			this.flagSet.add(optionFlag);
		}
		else {
			this.flagSet.remove(optionFlag);
		}
		return this;
	}

	public boolean supportStdoutHandler() {
		return !this.is(sender) && this.is(returnable) && (this.isRetType(StringType) || this.isRetType(TaskType));
	}

	public boolean supportStderrHandler() {
		return !this.is(sender) && this.is(throwable) || this.isRetType(TaskType);
	}

	public void setTimeout(String timeSymbol) {
		this.setFlag(timeout, true);
		this.time = Long.parseLong(timeSymbol.toString());
	}

	public long getTimeout() {
		return this.time;
	}

	@Override
	public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("<");
		sBuilder.append(this.retType.name());
		for(Behavior flag : this.flagSet) {
			sBuilder.append("|");
			sBuilder.append(flag.name());
		}
		sBuilder.append(">");
		return sBuilder.toString();
	}
}
