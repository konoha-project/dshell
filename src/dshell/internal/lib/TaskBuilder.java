package dshell.internal.lib;

import java.util.ArrayList;
import libbun.util.BArray;
import dshell.internal.grammar.DShellGrammar;
import dshell.internal.grammar.ShellGrammar;
import dshell.internal.remote.RequestSender;
import static dshell.internal.lib.TaskOption.Behavior.background;
import static dshell.internal.lib.TaskOption.Behavior.printable;
import static dshell.internal.lib.TaskOption.Behavior.returnable;
import static dshell.internal.lib.TaskOption.Behavior.throwable;
import static dshell.internal.lib.TaskOption.RetType.IntType;
import static dshell.internal.lib.TaskOption.RetType.StringType;
import static dshell.internal.lib.TaskOption.RetType.TaskArrayType;
import static dshell.internal.lib.TaskOption.RetType.TaskType;
import static dshell.internal.lib.TaskOption.RetType.VoidType;

public class TaskBuilder {
	// called by VisitCommandNode
	public static void execCommandVoid(CommandArg[][] cmds) {
		TaskOption option = TaskOption.of(VoidType, printable, throwable);
		TaskBuilder.createTask(toCmdsList(cmds), option);
	}

	public static int execCommandInt(CommandArg[][] cmds) {
		TaskOption option = TaskOption.of(IntType, printable, returnable);
		return ((Integer)TaskBuilder.createTask(toCmdsList(cmds), option)).intValue();
	}

	public static boolean execCommandBool(CommandArg[][] cmds) {
		return execCommandInt(cmds) == 0;
	}

	public static String execCommandString(CommandArg[][] cmds) {
		TaskOption option = TaskOption.of(StringType, returnable);
		return (String)TaskBuilder.createTask(toCmdsList(cmds), option);
	}

	public static BArray<String> execCommandStringArray(CommandArg[][] cmds) {
		return new BArray<String>(0, Utils.splitWithDelim(execCommandString(cmds)));
	}

	public static Task execCommandTask(CommandArg[][] cmds) {
		TaskOption option = TaskOption.of(TaskType, printable, returnable, throwable);
		return (Task)TaskBuilder.createTask(toCmdsList(cmds), option);
	}

	@SuppressWarnings("unchecked")
	public static BArray<Task> execCommandTaskArray(CommandArg[][] cmds) {
		TaskOption option = TaskOption.of(TaskArrayType, printable, returnable, throwable);
		return (BArray<Task>)TaskBuilder.createTask(toCmdsList(cmds), option);
	}

	public static Object createTask(ArrayList<ArrayList<CommandArg>> cmdsList, TaskOption option) {
		ArrayList<ArrayList<CommandArg>> newCmdsList = setInternalOption(option, cmdsList);
		PseudoProcess[] procs = createProcs(option, newCmdsList);
		final Task task = new Task(procs, option, toRepresent(procs, option));
		if(option.is(background)) {
			return (option.isRetType(TaskType) && option.is(returnable)) ? task : null;
		}
		task.join();
		if(option.is(returnable)) {
			if(option.isRetType(StringType)) {
				return task.getOutMessage();
			}
			else if(option.isRetType(IntType)) {
				return new Integer(task.getExitStatus());
			}
			else if(option.isRetType(TaskType)) {
				return task;
			}
			else if(option.isRetType(TaskArrayType)) {
				return Task.getTaskArray(task);
			}
		}
		return null;
	}

	private static String toRepresent(PseudoProcess[] procs, TaskOption option) {
		StringBuilder sBuilder = new StringBuilder();
		for(int i = 0; i< procs.length; i++) {
			if(i != 0) {
				sBuilder.append(",");
			}
			sBuilder.append(procs[i].toString());
		}
		sBuilder.append(" ");
		sBuilder.append(option.toString());
		return sBuilder.toString();
	}

	private static ArrayList<ArrayList<CommandArg>> setInternalOption(final TaskOption option, ArrayList<ArrayList<CommandArg>> cmdsList) {
		ArrayList<ArrayList<CommandArg>> newCmdsBuffer = new ArrayList<ArrayList<CommandArg>>();
		for(ArrayList<CommandArg> currentCmds : cmdsList) {
			if(currentCmds.get(0).eq(ShellGrammar.background)) {
				option.setFlag(background, option.isRetType(TaskType) || option.isRetType(VoidType));
				continue;
			}
			newCmdsBuffer.add(currentCmds);
		}
		return newCmdsBuffer;
	}

	private static PseudoProcess[] createProcs(final TaskOption option, ArrayList<ArrayList<CommandArg>> cmdsList) {
		ArrayList<PseudoProcess> procBuffer = new ArrayList<PseudoProcess>();
		boolean foundTraceOption = false;
		int size = cmdsList.size();
		for(int i = 0; i < size; i++) {
			ArrayList<CommandArg> currentCmds = cmdsList.get(i);
			CommandArg cmdSymbol = currentCmds.get(0);
			PseudoProcess prevProc = null;
			int currentbufferSize = procBuffer.size();
			if(currentbufferSize > 0) {
				prevProc = procBuffer.get(currentbufferSize - 1);
			}
			if(cmdSymbol.eq("<")) {
				prevProc.setInputRedirect(currentCmds.get(1));
			}
			else if(cmdSymbol.eq("1>") || cmdSymbol.eq(">")) {
				prevProc.setOutputRedirect(PseudoProcess.STDOUT_FILENO, currentCmds.get(1), false);
			}	
			else if(cmdSymbol.eq("1>>") || cmdSymbol.eq(">>")) {
				prevProc.setOutputRedirect(PseudoProcess.STDOUT_FILENO, currentCmds.get(1), true);
			}
			else if(cmdSymbol.eq("2>")) {
				prevProc.setOutputRedirect(PseudoProcess.STDERR_FILENO, currentCmds.get(1), false);
			}
			else if(cmdSymbol.eq("2>>")) {
				prevProc.setOutputRedirect(PseudoProcess.STDERR_FILENO, currentCmds.get(1), true);
			}
			else if(cmdSymbol.eq("2>&1")) {
				prevProc.mergeErrorToOut();
			}
			else if(cmdSymbol.eq("&>") || cmdSymbol.eq(">&")) {
				prevProc.mergeErrorToOut();
				prevProc.setOutputRedirect(PseudoProcess.STDOUT_FILENO, currentCmds.get(1), false);
			}
			else if(cmdSymbol.eq("&>>")) {
				prevProc.mergeErrorToOut();
				prevProc.setOutputRedirect(PseudoProcess.STDOUT_FILENO, currentCmds.get(1), true);
			}
			else if(cmdSymbol.eq(DShellGrammar.location)) {
				ArrayList<ArrayList<CommandArg>> sendingCmdsList = new ArrayList<ArrayList<CommandArg>>();
				for(int j = i + 1; j < size; j++) {
					sendingCmdsList.add(cmdsList.get(j));
				}
				PseudoProcess proc = new RequestSender(sendingCmdsList, option.is(background));
				proc.setArgumentList(currentCmds);
				procBuffer.add(proc);
				option.setFlag(background, false);
				break;
			}
			else if(cmdSymbol.eq(ShellGrammar.trace)) {
				foundTraceOption = true;
				continue;
			}
			else if(cmdSymbol.eq(ShellGrammar.timeout)) {
				option.setTimeout(currentCmds.get(1));
				continue;
			}
			else {
				if(foundTraceOption) {
					foundTraceOption = false;
					procBuffer.add(createProc(option, currentCmds, checkTraceRequirements()));
				}
				else {
					procBuffer.add(createProc(option, currentCmds, false));
				}
			}
		}
		int bufferSize = procBuffer.size();
		procBuffer.get(0).setFirstProcFlag(true);
		procBuffer.get(bufferSize - 1).setLastProcFlag(true);
		return procBuffer.toArray(new PseudoProcess[bufferSize]);
	}

	private static PseudoProcess createProc(final TaskOption option, ArrayList<CommandArg> cmds, boolean enableTrace) {
		PseudoProcess proc = RuntimeContext.getContext().getBuiltinCommand(cmds);
		if(proc != null) {
			return proc;
		}
		proc = new SubProc(option, enableTrace);
		proc.setArgumentList(cmds);
		return proc;
	}

	private static ArrayList<ArrayList<CommandArg>> toCmdsList(CommandArg[][] cmds) {
		ArrayList<ArrayList<CommandArg>> cmdsList = new ArrayList<ArrayList<CommandArg>>();
		for(CommandArg[] cmd : cmds) {
			ArrayList<CommandArg> cmdList = new ArrayList<CommandArg>();
			for(CommandArg tempCmd : cmd) {
				cmdList.add(tempCmd);
			}
			cmdsList.add(cmdList);
		}
		return cmdsList;
	}

	private static boolean checkTraceRequirements() {
		if(System.getProperty("os.name").equals("Linux")) {
			SubProc.traceBackendType = SubProc.traceBackend_ltrace;
			return Utils.getCommandFromPath("ltrace") != null;
		}
		System.err.println("Systemcall Trace is Not Supported");
		return false;
	}
}
