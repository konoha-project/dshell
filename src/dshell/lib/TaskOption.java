package dshell.lib;

import static dshell.lib.TaskOption.Behavior.returnable;
import static dshell.lib.TaskOption.Behavior.throwable;
import static dshell.lib.TaskOption.RetType.StringType;
import static dshell.lib.TaskOption.RetType.TaskType;

import java.util.EnumSet;

public class TaskOption {
	public static enum Behavior {
		returnable,
		printable ,
		throwable ,
		background,
		tracable ,
	}

	public static enum RetType {
		VoidType   ,
		IntType    ,
		BooleanType,
		StringType ,
		TaskType   ,
	}

	private final RetType retType;
	private final EnumSet<Behavior> flagSet;

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
		return this.is(returnable) && (this.isRetType(StringType) || this.isRetType(TaskType));
	}

	public boolean supportStderrHandler() {
		return this.is(throwable) || this.isRetType(TaskType);
	}

	@Override
	public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append(this.retType.name());
		for(Behavior flag : this.flagSet) {
			sBuilder.append("|" + flag.name());
		}
		return sBuilder.toString();
	}
}

class ProcOption {
	public static enum MergeType {
		withoutMerge,
		mergeErrorToOut,
		mergeOutToError,
	}

	public static enum OutputType {
		STDOUT_FILENO,
		STDERR_FILENO,
	}

	public static enum ProcPosition {
		firstProc,
		lastProc,
	}

	private MergeType mergeType;
	private EnumSet<ProcPosition> procPosition;

	public ProcOption() {
		this.mergeType = MergeType.withoutMerge;
		this.procPosition = EnumSet.noneOf(ProcPosition.class);
	}

	public void setMergeType(MergeType mergeType) {
		this.mergeType = mergeType;
	}

	public MergeType getMergeType() {
		return this.mergeType;
	}

	public void setProcPosition(ProcPosition... positions) {
		for(ProcPosition position : positions) {
			this.procPosition.add(position);
		}
	}

	public boolean isProcPosition(ProcPosition position) {
		return this.procPosition.contains(position);
	}
}
