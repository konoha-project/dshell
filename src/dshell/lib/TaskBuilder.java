package dshell.lib;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Calendar;

import libbun.util.BArray;

import dshell.grammar.DShellGrammar;
import dshell.grammar.ShellGrammar;
import dshell.remote.RequestSender;
import static dshell.lib.TaskOption.Behavior.returnable;
import static dshell.lib.TaskOption.Behavior.printable ;
import static dshell.lib.TaskOption.Behavior.throwable ;
import static dshell.lib.TaskOption.Behavior.background;
import static dshell.lib.TaskOption.RetType.VoidType   ;
import static dshell.lib.TaskOption.RetType.IntType    ;
import static dshell.lib.TaskOption.RetType.StringType ;
import static dshell.lib.TaskOption.RetType.TaskType   ;
import static dshell.lib.TaskOption.RetType.TaskArrayType;

public class TaskBuilder {
	private TaskOption option;
	private PseudoProcess[] Processes;
	private StringBuilder sBuilder;

	public TaskBuilder(ArrayList<ArrayList<CommandArg>> cmdsList, TaskOption option) {
		this.option = option;
		ArrayList<ArrayList<CommandArg>> newCmdsList = this.setInternalOption(cmdsList);
		this.Processes = this.createProcs(newCmdsList);
		// generate object representation
		this.sBuilder = new StringBuilder();
		for(int i = 0; i< this.Processes.length; i++) {
			if(i != 0) {
				this.sBuilder.append(",");
			}
			this.sBuilder.append(this.Processes[i].toString());
		}
		this.sBuilder.append(" <");
		this.sBuilder.append(this.option.toString());
		this.sBuilder.append(">");
	}

	public Object invoke() {
		Task task = new Task(this.Processes, this.option, this.sBuilder.toString());
		if(this.option.is(background)) {
			return (this.option.isRetType(TaskType) && this.option.is(returnable)) ? task : null;
		}
		task.join();
		if(this.option.is(returnable)) {
			if(this.option.isRetType(StringType)) {
				return task.getOutMessage();
			}
			else if(this.option.isRetType(IntType)) {
				return new Integer(task.getExitStatus());
			}
			else if(this.option.isRetType(TaskType)) {
				return task;
			}
			else if(this.option.isRetType(TaskArrayType)) {
				return Task.getTaskArray(task);
			}
		}
		return null;
	}

	public PseudoProcess[] getProcesses() {
		return this.Processes;
	}

	public TaskOption getOption() {
		return this.option;
	}

	@Override public String toString() {
		return this.sBuilder.toString();
	}

	private ArrayList<ArrayList<CommandArg>> setInternalOption(ArrayList<ArrayList<CommandArg>> cmdsList) {
		ArrayList<ArrayList<CommandArg>> newCmdsBuffer = new ArrayList<ArrayList<CommandArg>>();
		for(ArrayList<CommandArg> currentCmds : cmdsList) {
			if(currentCmds.get(0).eq(ShellGrammar.background)) {
				this.option.setFlag(background, this.option.isRetType(TaskType) || this.option.isRetType(VoidType));
				continue;
			}
			newCmdsBuffer.add(currentCmds);
		}
		return newCmdsBuffer;
	}

	private PseudoProcess[] createProcs(ArrayList<ArrayList<CommandArg>> cmdsList) {
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
				PseudoProcess proc = new RequestSender(sendingCmdsList, this.option.is(background));
				proc.setArgumentList(currentCmds);
				procBuffer.add(proc);
				this.option.setFlag(background, false);
				break;
			}
			else if(cmdSymbol.eq(ShellGrammar.trace)) {
				foundTraceOption = true;
				continue;
			}
			else if(cmdSymbol.eq(ShellGrammar.timeout)) {
				this.option.setTimeout(currentCmds.get(1));
				continue;
			}
			else {
				if(foundTraceOption) {
					foundTraceOption = false;
					procBuffer.add(this.createProc(currentCmds, checkTraceRequirements()));
				}
				else {
					procBuffer.add(this.createProc(currentCmds, false));
				}
			}
		}
		int bufferSize = procBuffer.size();
		procBuffer.get(0).setFirstProcFlag(true);
		procBuffer.get(bufferSize - 1).setLastProcFlag(true);
		return procBuffer.toArray(new PseudoProcess[bufferSize]);
	}

	private PseudoProcess createProc(ArrayList<CommandArg> cmds, boolean enableTrace) {
		PseudoProcess proc = BuiltinCommand.createCommand(cmds);
		if(proc != null) {
			return proc;
		}
		proc = new SubProc(this.option, enableTrace);
		proc.setArgumentList(cmds);
		return proc;
	}

	// called by ModifiedAsmGenerator#VisitCommandNode
	public static void ExecCommandVoid(CommandArg[][] cmds) {
		TaskOption option = TaskOption.of(VoidType, printable, throwable);
		new TaskBuilder(toCmdsList(cmds), option).invoke();
	}

	public static int ExecCommandInt(CommandArg[][] cmds) {
		TaskOption option = TaskOption.of(IntType, printable, returnable);
		return ((Integer)new TaskBuilder(toCmdsList(cmds), option).invoke()).intValue();
	}

	public static boolean ExecCommandBool(CommandArg[][] cmds) {
		return ExecCommandInt(cmds) == 0;
	}

	public static String ExecCommandString(CommandArg[][] cmds) {
		TaskOption option = TaskOption.of(StringType, returnable);
		return (String)new TaskBuilder(toCmdsList(cmds), option).invoke();
	}

	public static BArray<String> ExecCommandStringArray(CommandArg[][] cmds) {
		return new BArray<String>(0, Utils.splitWithDelim(ExecCommandString(cmds)));
	}

	public static Task ExecCommandTask(CommandArg[][] cmds) {
		TaskOption option = TaskOption.of(TaskType, printable, returnable, throwable);
		return (Task)new TaskBuilder(toCmdsList(cmds), option).invoke();
	}

	@SuppressWarnings("unchecked")
	public static BArray<Task> ExecCommandTaskArray(CommandArg[][] cmds) {
		TaskOption option = TaskOption.of(TaskArrayType, printable, returnable, throwable);
		return (BArray<Task>)new TaskBuilder(toCmdsList(cmds), option).invoke();
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

class SubProc extends PseudoProcess {
	public final static int traceBackend_ltrace = 0;
	public static int traceBackendType = traceBackend_ltrace;

	private final static String logdirPath = "/tmp/dshell-trace-log";
	private static int logId = 0;

	private ProcessBuilder procBuilder;
	private Process proc;
	private TaskOption taskOption;
	public boolean isKilled = false;
	private boolean enableTrace = false;
	public String logFilePath = null;

	private static String createLogNameHeader() {
		Calendar cal = Calendar.getInstance();
		StringBuilder logNameHeader = new StringBuilder();
		logNameHeader.append(cal.get(Calendar.YEAR) + "-");
		logNameHeader.append((cal.get(Calendar.MONTH) + 1) + "-");
		logNameHeader.append(cal.get(Calendar.DATE) + "-");
		logNameHeader.append(cal.get((Calendar.HOUR) + 1) + ":");
		logNameHeader.append(cal.get(Calendar.MINUTE) + "-");
		logNameHeader.append(cal.get(Calendar.MILLISECOND));
		logNameHeader.append("-" + logId++);
		return logNameHeader.toString();
	}

	public SubProc(TaskOption taskOption, boolean enableTrace) {
		super();
		this.taskOption = taskOption;
		this.enableTrace = enableTrace;
		this.initTrace();
	}

	private void initTrace() {
		if(this.enableTrace) {
			logFilePath = new String(logdirPath + "/" + createLogNameHeader() + ".log");
			new File(logdirPath).mkdir();
			String[] traceCmds;
			if(traceBackendType == traceBackend_ltrace) {
				traceCmds = new String[] {"ltrace", "-f", "-S", "-o", logFilePath};
			}
			else {
				Utils.fatal(1, "invalid trace backend type");
				return;
			}
			for(String traceCmd : traceCmds) {
				this.commandList.add(traceCmd);
			}
		}
	}

	@Override
	public void setArgumentList(ArrayList<CommandArg> argList) {
		CommandArg arg = argList.get(0);
		this.cmdNameBuilder.append(arg);
		if(arg.eq("sudo")) {
			ArrayList<String> newCommandList = new ArrayList<String>();
			newCommandList.add(arg.toString());
			for(String cmd : this.commandList) {
				newCommandList.add(cmd);
			}
			this.commandList = newCommandList;
		}
		else {
			this.addToCommandList(arg);
		}
		this.sBuilder.append("[");
		this.sBuilder.append(arg);
		int size = argList.size();
		for(int i = 1; i < size; i++) {
			arg = argList.get(i);
			this.addToCommandList(arg);
			this.cmdNameBuilder.append(" " + arg);
			this.sBuilder.append(", ");
			this.sBuilder.append(arg);
		}
		this.sBuilder.append("]");
		this.procBuilder = new ProcessBuilder(this.commandList.toArray(new String[this.commandList.size()]));
		this.procBuilder.redirectError(Redirect.INHERIT);
		this.stderrIsDirty = true;
	}

	@Override
	public void start() {
		try {
			this.setStreamBehavior();
			this.proc = procBuilder.start();
			this.stdin = this.proc.getOutputStream();
			this.stdout = this.proc.getInputStream();
			this.stderr = this.proc.getErrorStream();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void mergeErrorToOut() {
		this.procBuilder.redirectErrorStream(true);
		this.sBuilder.append("&");
		this.stderrIsDirty = true;
	}

	private void setStreamBehavior() {
		if(this.isFirstProc) {
			if(this.procBuilder.redirectInput().file() == null) {
				procBuilder.redirectInput(Redirect.INHERIT);
				this.stdinIsDirty = true;
			}
		}
		if(this.isLastProc) {
			if(this.procBuilder.redirectOutput().file() == null && !this.taskOption.supportStdoutHandler()) {
				procBuilder.redirectOutput(Redirect.INHERIT);
				this.stdoutIsDirty = true;
			}
		}
		if(this.procBuilder.redirectError().file() == null && this.taskOption.supportStderrHandler()) {
			this.procBuilder.redirectError(Redirect.PIPE);
			this.stderrIsDirty = false;
		}
	}

	@Override
	public void setInputRedirect(CommandArg readFileName) {
		this.stdinIsDirty = true;
		this.procBuilder.redirectInput(new File(readFileName.toString()));
		this.sBuilder.append(" <");
		this.sBuilder.append(readFileName);
	}

	@Override
	public void setOutputRedirect(int fd, CommandArg writeFileName, boolean append) {
		File file = new File(writeFileName.toString());
		Redirect redirDest = Redirect.to(file);
		if(append) {
			redirDest = Redirect.appendTo(file);
		}
		if(fd == STDOUT_FILENO) {
			this.stdoutIsDirty = true;
			this.procBuilder.redirectOutput(redirDest);
		} 
		else if(fd == STDERR_FILENO) {
			this.stderrIsDirty = true;
			this.procBuilder.redirectError(redirDest);
		}
		this.sBuilder.append(" " + fd);
		this.sBuilder.append(">");
		if(append) {
			this.sBuilder.append(">");
		}
		this.sBuilder.append(writeFileName);
	}

	@Override
	public void waitTermination() {
		try {
			this.retValue = this.proc.waitFor();
		}
		catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void kill() {
		if(System.getProperty("os.name").startsWith("Windows")) {
			this.proc.destroy();
			return;
		} 
		try {
			int pid = (Integer) Utils.getValue(this.proc, "pid");
			String[] cmds = {"kill", "-9", Integer.toString(pid)};
			Process procKiller = new ProcessBuilder(cmds).start();
			procKiller.waitFor();
			this.isKilled = true;
		}
		catch(Exception e) {
			e.printStackTrace();
			Utils.fatal(1, "killing process problem");
		}
	}

	public boolean checkTermination() {
		try {
			this.retValue = this.proc.exitValue();
			return true;
		}
		catch(IllegalThreadStateException e) {
			return false;
		}
	}

	public String getLogFilePath() {
		return this.logFilePath;
	}

	public void deleteLogFile() {
		if(this.logFilePath != null) {
			new File(this.logFilePath).delete();
		}
	}

	@Override public boolean isTraced() {
		return this.enableTrace;
	}
}
