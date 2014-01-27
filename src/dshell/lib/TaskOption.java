package dshell.lib;

import java.util.EnumSet;

public class TaskOption {
	public static enum Global {
		returnable,
		printable ,
		throwable ,
		background,
		inference ,
	}

	public static enum RetType {
		VoidType   ,
		IntType    ,
		BooleanType,
		StringType ,
		TaskType   ,
	}

	private final RetType retType;
	private final EnumSet<Global> flagSet;

	private TaskOption(RetType retType, EnumSet<Global> flagSet) {
		this.retType = retType;
		this.flagSet = flagSet;
	}

	public static TaskOption of(RetType retType, Global... optionFlags) {
		EnumSet<Global> flagSet = EnumSet.noneOf(Global.class);
		for(Global flag : optionFlags) {
			flagSet.add(flag);
		}
		TaskOption option = new TaskOption(retType, flagSet);
		return option;
	}

	public RetType getRetType() {
		return this.retType;
	}

	public boolean is(Global optionFlag) {
		return this.flagSet.contains(optionFlag);
	}

	public void setFlag(Global optionFlag, boolean set) {
		if(set) {
			this.flagSet.add(optionFlag);
		}
		else {
			this.flagSet.remove(optionFlag);
		}
	}

	@Override
	public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append(this.retType.name());
		for(Global flag : this.flagSet) {
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

	private MergeType mergeType = MergeType.withoutMerge;

	public void setMergeType(MergeType mergeType) {
		this.mergeType = mergeType;
	}

	public MergeType getMergeType() {
		return this.mergeType;
	}
}
