package dshell.lib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;

import dshell.lang.DShellGrammar;
import dshell.remote.RemoteContext;
import dshell.remote.RemoteProcClient;
import dshell.remote.RemoteProcServer;
import dshell.util.Utils;

import static dshell.lib.TaskOption.Behavior.returnable;
import static dshell.lib.TaskOption.Behavior.printable ;
import static dshell.lib.TaskOption.Behavior.throwable ;
import static dshell.lib.TaskOption.Behavior.background;
import static dshell.lib.TaskOption.Behavior.client;
import static dshell.lib.TaskOption.Behavior.server;

import static dshell.lib.TaskOption.RetType.VoidType   ;
import static dshell.lib.TaskOption.RetType.IntType    ;
import static dshell.lib.TaskOption.RetType.BooleanType;
import static dshell.lib.TaskOption.RetType.StringType ;
import static dshell.lib.TaskOption.RetType.TaskType   ;

public class TaskBuilder {
	private TaskOption option;
	private PseudoProcess[] Processes;
	private long timeout = -1;
	private StringBuilder sBuilder;

	public final RemoteContext context;
	public OutputStream remoteOutStream = null;
	public OutputStream remoteErrStream = null;

	public TaskBuilder(ArrayList<ArrayList<String>> cmdsList, TaskOption option) {
		this(null, cmdsList, option);
	}

	public TaskBuilder(RemoteContext context, ArrayList<ArrayList<String>> cmdsList, TaskOption option) {
		this.context = context;
		this.option = option;
		ArrayList<ArrayList<String>> newCmdsList = this.setInternalOption(cmdsList);
		this.Processes = this.createProcs(newCmdsList);
		// generate object representation
		this.sBuilder = new StringBuilder();
		for(int i = 0; i< this.Processes.length; i++) {
			if(i != 0) {
				this.sBuilder.append(",\n");
			}
			this.sBuilder.append("{");
			this.sBuilder.append(this.Processes[i].toString());
			this.sBuilder.append("}");
		}
		this.sBuilder.append("\n<");
		this.sBuilder.append(this.option.toString());
		this.sBuilder.append(">");
	}

	public Object invoke() {
		Task task = new Task(this);
		if(this.option.is(background)) {
			return (this.option.isRetType(TaskType) && this.option.is(returnable)) ? task : null;
		}
		task.join();
		if(this.option.is(returnable)) {
			if(this.option.isRetType(StringType)) {
				return task.getOutMessage();
			}
			else if(this.option.isRetType(BooleanType)) {
				return new Boolean(task.getExitStatus() == 0);
			}
			else if(this.option.isRetType(IntType)) {
				return new Integer(task.getExitStatus());
			}
			else if(this.option.isRetType(TaskType)) {
				return task;
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

	public long getTimeout() {
		return this.timeout;
	}

	@Override public String toString() {
		return this.sBuilder.toString();
	}

	private ArrayList<ArrayList<String>> setInternalOption(ArrayList<ArrayList<String>> cmdsList) {
		ArrayList<ArrayList<String>> newCmdsBuffer = new ArrayList<ArrayList<String>>();
		for(ArrayList<String> currentCmds : cmdsList) {
			if(currentCmds.get(0).equals(DShellGrammar.timeout)) {
				StringBuilder numBuilder = new StringBuilder();
				StringBuilder unitBuilder = new StringBuilder();
				int len = currentCmds.get(1).length();
				for(int j = 0; j < len; j++) {
					char ch = currentCmds.get(1).charAt(j);
					if(Character.isDigit(ch)) {
						numBuilder.append(ch);
					}
					else {
						unitBuilder.append(ch);
					}
				}
				long num = Integer.parseInt(numBuilder.toString());
				String unit = unitBuilder.toString();
				if(unit.equals("s")) {
					num = num * 1000;
				}
				if(num >= 0) {
					this.timeout = num;
				}
				int baseIndex = 2;
				ArrayList<String> newCmds = new ArrayList<String>();
				int size = currentCmds.size();
				for(int j = baseIndex; j < size; j++) {
					newCmds.add(currentCmds.get(j));
				}
				currentCmds = newCmds;
			}
			else if(currentCmds.get(0).equals(DShellGrammar.background)) {
				this.option.setFlag(background, true);
				continue;
			}
			newCmdsBuffer.add(currentCmds);
		}
		return newCmdsBuffer;
	}

	private PseudoProcess[] createProcs(ArrayList<ArrayList<String>> cmdsList) {
		ArrayList<PseudoProcess> procBuffer = new ArrayList<PseudoProcess>();
		if(this.option.is(server) && this.context != null) {
			this.option.setFlag(background, false);	// TODO: background
			this.timeout = -1;	// TODO: timeout
			RemoteProcServer proc = new RemoteProcServer(this.context);
			procBuffer.add(proc);
			this.remoteOutStream = proc.outStream;
			this.remoteErrStream = proc.errStream;
		}
		int size = cmdsList.size();
		for(int i = 0; i < size; i++) {
			ArrayList<String> currentCmds = cmdsList.get(i);
			String cmdSymbol = currentCmds.get(0);
			PseudoProcess prevProc = null;
			int currentbufferSize = procBuffer.size();
			if(currentbufferSize > 0) {
				prevProc = procBuffer.get(currentbufferSize - 1);
			}
			if(cmdSymbol.equals("<")) {
				prevProc.setInputRedirect(currentCmds.get(1));
			}
			else if(cmdSymbol.equals("1>") || cmdSymbol.equals(">")) {
				prevProc.setOutputRedirect(PseudoProcess.STDOUT_FILENO, currentCmds.get(1), false);
			}	
			else if(cmdSymbol.equals("1>>") || cmdSymbol.equals(">>")) {
				prevProc.setOutputRedirect(PseudoProcess.STDOUT_FILENO, currentCmds.get(1), true);
			}
			else if(cmdSymbol.equals("2>")) {
				prevProc.setOutputRedirect(PseudoProcess.STDERR_FILENO, currentCmds.get(1), false);
			}
			else if(cmdSymbol.equals("2>>")) {
				prevProc.setOutputRedirect(PseudoProcess.STDERR_FILENO, currentCmds.get(1), true);
			}
			else if(cmdSymbol.equals("&>") || cmdSymbol.equals(">&")) {
				prevProc.setOutputRedirect(PseudoProcess.STDOUT_FILENO, currentCmds.get(1), false);
				prevProc.mergeErrorToOut();
			}
			else if(cmdSymbol.equals("&>>")) {
				prevProc.setOutputRedirect(PseudoProcess.STDOUT_FILENO, currentCmds.get(1), true);
				prevProc.mergeErrorToOut();
			}
			else if(cmdSymbol.equals(DShellGrammar.location)) {
				this.option.setFlag(client, true);
				RemoteProcClient proc = new RemoteProcClient(this.option);
				ArrayList<ArrayList<String>> subList = new ArrayList<ArrayList<String>>();
				for(int index = i + 1; index < size; index++) {
					subList.add(cmdsList.get(index));
				}
				proc.setArgumentList(currentCmds, subList);
				procBuffer.add(proc);
				break;
			}
			else if(cmdSymbol.equals(DShellGrammar.trace)) {
				int cmdSize = currentCmds.size();
				ArrayList<String> newCmds = new ArrayList<String>();
				for(int index = 1; index < cmdSize; index++) {
					newCmds.add(currentCmds.get(index));
				}
				procBuffer.add(this.createProc(newCmds, checkTraceRequirements()));
			}
			else {
				procBuffer.add(this.createProc(currentCmds, false));
			}
		}
		int bufferSize = procBuffer.size();
		procBuffer.get(0).setFirstProcFlag(true);
		procBuffer.get(bufferSize - 1).setLastProcFlag(true);
		return procBuffer.toArray(new PseudoProcess[bufferSize]);
	}

	private PseudoProcess createProc(ArrayList<String> cmds, boolean enableTrace) {
		PseudoProcess proc = BuiltinCommand.createCommand(cmds);
		if(proc != null) {
			return proc;
		}
		proc = new SubProc(this.option, enableTrace);
		proc.setArgumentList(cmds);
		return proc;
	}

	// called by ModifiedReflectionEngine#VisitCommandNode
	public static void ExecCommandVoidTopLevel(String[][] cmds) {
		TaskOption option = TaskOption.of(VoidType, printable);
		new TaskBuilder(toCmdsList(cmds), option).invoke();
	}

	public static int ExecCommandIntTopLevel(String[][] cmds) {
		TaskOption option = TaskOption.of(IntType, printable, returnable);
		return ((Integer)new TaskBuilder(toCmdsList(cmds), option).invoke()).intValue();
	}

	public static boolean ExecCommandBoolTopLevel(String[][] cmds) {
		TaskOption option = TaskOption.of(BooleanType, printable, returnable);
		return ((Boolean)new TaskBuilder(toCmdsList(cmds), option).invoke()).booleanValue();
	}

	public static String ExecCommandStringTopLevel(String[][] cmds) {
		TaskOption option = TaskOption.of(StringType, returnable);
		return (String)new TaskBuilder(toCmdsList(cmds), option).invoke();
	}

	public static Task ExecCommandTaskTopLevel(String[][] cmds) {
		TaskOption option = TaskOption.of(TaskType, printable, returnable);
		return (Task)new TaskBuilder(toCmdsList(cmds), option).invoke();
	}
	// called by ModifiedJavaByteCodeGenerator#VisitCommandNode
	public static void ExecCommandVoid(String[][] cmds) {
		TaskOption option = TaskOption.of(VoidType, printable, throwable);
		new TaskBuilder(toCmdsList(cmds), option).invoke();
	}

	public static int ExecCommandInt(String[][] cmds) {
		TaskOption option = TaskOption.of(IntType, printable, returnable);
		return ((Integer)new TaskBuilder(toCmdsList(cmds), option).invoke()).intValue();
	}

	public static boolean ExecCommandBool(String[][] cmds) {
		TaskOption option = TaskOption.of(BooleanType, printable, returnable);
		return ((Boolean)new TaskBuilder(toCmdsList(cmds), option).invoke()).booleanValue();
	}

	public static String ExecCommandString(String[][] cmds) {
		TaskOption option = TaskOption.of(StringType, returnable);
		return (String)new TaskBuilder(toCmdsList(cmds), option).invoke();
	}

	public static Task ExecCommandTask(String[][] cmds) {
		TaskOption option = TaskOption.of(TaskType, printable, returnable, throwable);
		return (Task)new TaskBuilder(toCmdsList(cmds), option).invoke();
	}

	private static ArrayList<ArrayList<String>> toCmdsList(String[][] cmds) {
		ArrayList<ArrayList<String>> cmdsList = new ArrayList<ArrayList<String>>();
		for(String[] cmd : cmds) {
			ArrayList<String> cmdList = new ArrayList<String>();
			for(String tempCmd : cmd) {
				cmdList.add(tempCmd);
			}
			cmdsList.add(cmdList);
		}
		return cmdsList;
	}

	private static boolean checkTraceRequirements() {
		if(System.getProperty("os.name").equals("Linux")) {
			SubProc.traceBackendType = SubProc.traceBackend_ltrace;
			return Utils.isUnixCommand("ltrace");
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
			String[] traceCmd;
			if(traceBackendType == traceBackend_ltrace) {
				String[] backend_strace = {"ltrace", "-t", "-f", "-S", "-o", logFilePath};
				traceCmd = backend_strace;
			}
			else {
				throw new RuntimeException("invalid trace backend type");
			}
			for(int i = 0; i < traceCmd.length; i++) {
				this.commandList.add(traceCmd[i]);
			}
		}
	}

	@Override
	public void setArgumentList(ArrayList<String> argList) {
		String arg = argList.get(0);
		this.cmdNameBuilder.append(arg);
		if(arg.equals("sudo")) {
			ArrayList<String> newCommandList = new ArrayList<String>();
			newCommandList.add(arg);
			for(String cmd : this.commandList) {
				newCommandList.add(cmd);
			}
			this.commandList = newCommandList;
		}
		else {
			this.commandList.add(arg);
		}
		this.sBuilder.append("[");
		this.sBuilder.append(arg);
		int size = argList.size();
		for(int i = 1; i < size; i++) {
			arg = argList.get(i);
			this.commandList.add(arg);
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
		this.sBuilder.append("mergeErrorToOut");
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
	public void setInputRedirect(String readFileName) {
		this.stdinIsDirty = true;
		this.procBuilder.redirectInput(new File(readFileName));
		this.sBuilder.append(" <");
		this.sBuilder.append(readFileName);
	}

	@Override
	public void setOutputRedirect(int fd, String writeFileName, boolean append) {
		File file = new File(writeFileName);
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
			// get target pid
			Field pidField = this.proc.getClass().getDeclaredField("pid");
			pidField.setAccessible(true);
			int pid = pidField.getInt(this.proc);
			
			// kill process
			String[] cmds = {"kill", "-9", Integer.toString(pid)};
			Process procKiller = new ProcessBuilder(cmds).start();
			procKiller.waitFor();
			this.isKilled = true;
			//LibGreenTea.print("[killed]: " + this.getCmdName());
		}
		catch(Exception e) {
			throw new RuntimeException(e);
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

// copied from http://blog.art-of-coding.eu/piping-between-processes/
class PipeStreamHandler extends Thread {
	public final static int defaultBufferSize = 512;
	private InputStream input;
	private OutputStream[] outputs;
	private boolean closeInput;
	private boolean[] closeOutputs;

	public PipeStreamHandler(InputStream input, OutputStream output, boolean closeStream) {
		this.input = input;
		this.outputs = new OutputStream[1];
		this.outputs[0] = output;
		if(output == null) {
			this.outputs[0] = new NullStream();
		}
		this.closeInput = closeStream;
		this.closeOutputs = new boolean[1];
		this.closeOutputs[0] = closeStream;
	}

	public PipeStreamHandler(InputStream input, OutputStream[] outputs, boolean closeInput, boolean[] closeOutputs) {
		this.input = input;
		this.outputs = new OutputStream[outputs.length];
		this.closeInput = closeInput;
		this.closeOutputs = closeOutputs;
		for(int i = 0; i < this.outputs.length; i++) {
			this.outputs[i] = (outputs[i] == null) ? new NullStream() : outputs[i];
		}
	}

	@Override
	public void run() {
		if(this.input == null) {
			return;
		}
		try {
			byte[] buffer = new byte[defaultBufferSize];
			int read = 0;
			while(read > -1) {
				read = this.input.read(buffer, 0, buffer.length);
				if(read > -1) {
					for(int i = 0; i < this.outputs.length; i++) {
						this.outputs[i].write(buffer, 0, read);
					}
				}
			}
			if(this.closeInput) {
				this.input.close();
			}
			for(int i = 0; i < this.outputs.length; i++) {
				if(this.closeOutputs[i]) {
					this.outputs[i].close();
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	class NullStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {	// do nothing
		}
		@Override
		public void close() {	//do nothing
		}
	}
}

