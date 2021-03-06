package dshell.internal.lib;

import static dshell.internal.lib.TaskOption.Behavior.returnable;
import static dshell.internal.lib.TaskOption.Behavior.sender;
import static dshell.internal.lib.TaskOption.Behavior.throwable;
import static dshell.internal.lib.TaskOption.Behavior.timeout;
import static dshell.internal.lib.TaskOption.RetType.StringType;
import static dshell.internal.lib.TaskOption.RetType.TaskType;

import java.io.Serializable;
import java.util.EnumSet;

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

	private final RetType retType;
	private final EnumSet<Behavior> flagSet;
	private long time = -1;

	private TaskOption(RetType retType, EnumSet<Behavior> flagSet) {
		this.retType = retType;
		this.flagSet = flagSet;
	}

	public static TaskOption of(RetType retType, Behavior... optionFlags) {
		EnumSet<Behavior> flagSet = EnumSet.noneOf(Behavior.class);
		for(Behavior flag : optionFlags) {
			flagSet.add(flag);
		}
		TaskOption option = new TaskOption(retType, flagSet);
		return option;
	}

	public boolean isRetType(RetType type) {
		return this.retType == type;
	}

	public boolean is(Behavior optionFlag) {
		return this.flagSet.contains(optionFlag);
	}

	public void setFlag(Behavior optionFlag, boolean set) {
		if(set) {
			this.flagSet.add(optionFlag);
		}
		else {
			this.flagSet.remove(optionFlag);
		}
	}

	public boolean supportStdoutHandler() {
		return !this.is(sender) && this.is(returnable) && (this.isRetType(StringType) || this.isRetType(TaskType));
	}

	public boolean supportStderrHandler() {
		return !this.is(sender) && this.is(throwable) || this.isRetType(TaskType);
	}

	public void setTimeout(CommandArg timeSymbol) {
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
