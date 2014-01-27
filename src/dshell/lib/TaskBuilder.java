package dshell.lib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;

import dshell.lang.DShellGrammar;
import dshell.lib.ProcOption.MergeType;
import dshell.lib.ProcOption.OutputType;
import dshell.util.Utils;

import static dshell.lib.TaskOption.Global.returnable;
import static dshell.lib.TaskOption.Global.printable ;
import static dshell.lib.TaskOption.Global.throwable ;
import static dshell.lib.TaskOption.Global.background;
import static dshell.lib.TaskOption.Global.inference ;

import static dshell.lib.TaskOption.RetType.VoidType   ;
import static dshell.lib.TaskOption.RetType.IntType    ;
import static dshell.lib.TaskOption.RetType.BooleanType;
import static dshell.lib.TaskOption.RetType.StringType ;
import static dshell.lib.TaskOption.RetType.TaskType   ;

import static dshell.lib.ProcOption.MergeType.mergeErrorToOut;
import static dshell.lib.ProcOption.MergeType.mergeOutToError;

import static dshell.lib.ProcOption.OutputType.STDOUT_FILENO;
import static dshell.lib.ProcOption.OutputType.STDERR_FILENO;;

public class TaskBuilder {
	private TaskOption option;
	private PseudoProcess[] Processes;
	private long timeout = -1;
	private StringBuilder sBuilder;

	public MessageStreamHandler stdoutHandler;
	public MessageStreamHandler stderrHandler;

	public TaskBuilder(ArrayList<ArrayList<String>> cmdsList, TaskOption option) {
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
			return (this.option.getRetType() == TaskType) && this.option.is(returnable) ? task : null;
		}
		task.join();
		if(this.option.is(returnable)) {
			switch(this.option.getRetType()) {
			case StringType:
				return task.getOutMessage();
			case BooleanType:
				return new Boolean(task.getExitStatus() == 0);
			case IntType:
				return new Integer(task.getExitStatus());
			case TaskType:
				return task;
			case VoidType:
				break;
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
		boolean enableTrace = false;
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
			else if(currentCmds.get(0).equals("trace")) {
				enableTrace = checkTraceRequirements();
				int baseIndex = 1;
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
			else if(currentCmds.get(0).equals(DShellGrammar.errorAction_raise)) {
				this.option.setFlag(throwable, true);
				continue;
			}
			else if(currentCmds.get(0).equals(DShellGrammar.errorAction_trace)) {
				this.option.setFlag(throwable, true);
				this.option.setFlag(inference, true);
				enableTrace = checkTraceRequirements();
				continue;
			}
			newCmdsBuffer.add(currentCmds);
		}
		if(this.option.is(throwable)) {
			this.option.setFlag(inference, enableTrace);
		}
		return newCmdsBuffer;
	}

	private PseudoProcess[] createProcs(ArrayList<ArrayList<String>> cmdsList) {
		boolean enableSyscallTrace = this.option.is(inference);
		ArrayList<PseudoProcess> procBuffer = new ArrayList<PseudoProcess>();
		for(ArrayList<String> currentCmds : cmdsList) {
			String cmdSymbol = currentCmds.get(0);
			SubProc prevProc = null;
			int size = procBuffer.size();
			if(size > 0) {
				prevProc = (SubProc)procBuffer.get(size - 1);
			}
			if(cmdSymbol.equals("<")) {
				prevProc.setInputRedirect(currentCmds.get(1));
			}
			else if(cmdSymbol.equals("1>") || cmdSymbol.equals(">")) {
				prevProc.setOutputRedirect(STDOUT_FILENO, currentCmds.get(1), false);
			}	
			else if(cmdSymbol.equals("1>>") || cmdSymbol.equals(">>")) {
				prevProc.setOutputRedirect(STDOUT_FILENO, currentCmds.get(1), true);
			}
			else if(cmdSymbol.equals("2>")) {
				prevProc.setOutputRedirect(STDERR_FILENO, currentCmds.get(1), false);
			}
			else if(cmdSymbol.equals("2>>")) {
				prevProc.setOutputRedirect(STDERR_FILENO, currentCmds.get(1), true);
			}
			else if(cmdSymbol.equals("&>") || cmdSymbol.equals(">&")) {
				prevProc.setOutputRedirect(STDOUT_FILENO, currentCmds.get(1), false);
				prevProc.setMergeType(mergeErrorToOut);
			}
			else if(cmdSymbol.equals("&>>")) {
				prevProc.setOutputRedirect(STDOUT_FILENO, currentCmds.get(1), true);
				prevProc.setMergeType(mergeErrorToOut);
			}
			else if(cmdSymbol.equals(">&1") || cmdSymbol.equals("1>&1") || cmdSymbol.equals("2>&2")) {
				// do nothing
			}
			else if(cmdSymbol.equals("1>&2")) {
				prevProc.setMergeType(mergeOutToError);
			}
			else if(cmdSymbol.equals("2>&1")) {
				prevProc.setMergeType(mergeErrorToOut);
			}
			else {
				SubProc proc = new SubProc(enableSyscallTrace);
				proc.setArgumentList(currentCmds);
				procBuffer.add(proc);
			}
		}
		return procBuffer.toArray(new PseudoProcess[procBuffer.size()]);
	}

	// called by ModifiedJavaScriptSourceGenerator#VisitCommandNode 
	public static void ExecCommandVoidJS(ArrayList<ArrayList<String>> cmdsList) {
		TaskOption option = TaskOption.of(VoidType, printable);
		new TaskBuilder(cmdsList, option).invoke();
	}

	public static int ExecCommandIntJS(ArrayList<ArrayList<String>> cmdsList) {
		TaskOption option = TaskOption.of(IntType, printable, returnable);
		return ((Integer)new TaskBuilder(cmdsList, option).invoke()).intValue();
	}

	public static boolean ExecCommandBoolJS(ArrayList<ArrayList<String>> cmdsList) {
		TaskOption option = TaskOption.of(BooleanType, printable, returnable);
		return ((Boolean)new TaskBuilder(cmdsList, option).invoke()).booleanValue();
	}

	public static String ExecCommandStringJS(ArrayList<ArrayList<String>> cmdsList) {
		TaskOption option = TaskOption.of(StringType, returnable);
		return (String)new TaskBuilder(cmdsList, option).invoke();
	}

	public static Task ExecCommandTaskJS(ArrayList<ArrayList<String>> cmdsList) {
		TaskOption option = TaskOption.of(TaskType, printable, returnable);
		return (Task)new TaskBuilder(cmdsList, option).invoke();
	}

	// called by ModifiedJavaByteCodeGenerator#VisitCommandNode
	public static void ExecCommandVoid(String[][] cmds) {
		TaskOption option = TaskOption.of(VoidType, printable);
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

abstract class PseudoProcess {
	protected OutputStream stdin = null;
	protected InputStream stdout = null;
	protected InputStream stderr = null;

	protected StringBuilder cmdNameBuilder;
	protected ArrayList<String> commandList;
	protected StringBuilder sBuilder;

	protected boolean stdoutIsDirty = false;
	protected boolean stderrIsDirty = false;

	protected int retValue = 0;

	public PseudoProcess() {
		this.cmdNameBuilder = new StringBuilder();
		this.commandList = new ArrayList<String>();
		this.sBuilder = new StringBuilder();
	}

	public void setArgumentList(ArrayList<String> argList) {
		this.commandList = argList;
		int size = this.commandList.size();
		for(int i = 0; i < size; i++) {
			if(i != 0) {
				this.cmdNameBuilder.append(" ");
			}
			this.cmdNameBuilder.append(this.commandList.get(i));
		}
	}

	abstract public void start();

	public void pipe(PseudoProcess srcProc) {
		new PipeStreamHandler(srcProc.accessOutStream(), this.stdin, true).start();
	}

	abstract public void kill();

	public void waitTermination() {
	}

	public InputStream accessOutStream() {
		if(!this.stdoutIsDirty) {
			this.stdoutIsDirty = true;
			return this.stdout;
		}
		return null;
	}

	public InputStream accessErrorStream() {
		if(!this.stderrIsDirty) {
			this.stderrIsDirty = true;
			return this.stderr;
		}
		return null;
	}

	public int getRet() {
		return this.retValue;
	}

	public String getCmdName() {
		return this.cmdNameBuilder.toString();
	}

	public boolean isTraced() {
		return false;
	}

	@Override public String toString() {
		return this.sBuilder.toString();
	}
}

class SubProc extends PseudoProcess {
	public final static int traceBackend_ltrace = 0;
	public static int traceBackendType = traceBackend_ltrace;

	private final static String logdirPath = "/tmp/dshell-trace-log";
	private static int logId = 0;

	private Process proc;
	private ProcOption procOption;
	private boolean enableSyscallTrace = false;
	public boolean isKilled = false;
	public String logFilePath = null;

	private FileInputStream inFileStream = null;
	private FileOutputStream outFileStream = null;
	private FileOutputStream errFileStream = null;

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

	public SubProc(boolean enableSyscallTrace) {
		super();
		this.enableSyscallTrace = enableSyscallTrace;
		this.procOption = new ProcOption();
		initTrace();
	}

	private void initTrace() {
		if(this.enableSyscallTrace) {
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

	@Override public void setArgumentList(ArrayList<String> argList) {
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
	}

	@Override public void start() {
		try {
			ProcessBuilder procBuilder = new ProcessBuilder(this.commandList.toArray(new String[this.commandList.size()]));
			if(this.procOption.getMergeType() == mergeErrorToOut || this.procOption.getMergeType() == mergeOutToError) {
				procBuilder.redirectErrorStream(true);
			}
			this.proc = procBuilder.start();
			this.stdin = this.proc.getOutputStream();
			if(this.procOption.getMergeType() == mergeOutToError) {
				this.stdout = this.proc.getErrorStream();
				this.stderr = this.proc.getInputStream();
			}
			else {
				this.stdout = this.proc.getInputStream();
				this.stderr = this.proc.getErrorStream();
			}
			// input & output redirect
			this.readFile();
			this.writeFile(STDOUT_FILENO);
			this.writeFile(STDERR_FILENO);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setMergeType(MergeType mergeType) {
		this.procOption.setMergeType(mergeType);
		switch(this.procOption.getMergeType()) {
		case withoutMerge: break;
		case mergeErrorToOut:
			this.sBuilder.append(" 2>&1");
			break;
		case mergeOutToError:
			this.sBuilder.append(" 1>&2");
			break;
		}
	}

	public void setInputRedirect(String readFileName) {
		this.sBuilder.append(" <");
		this.sBuilder.append(readFileName);
		try {
			this.inFileStream = new FileInputStream(readFileName);
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void readFile() {
		if(this.inFileStream == null) {
			return;
		}
		InputStream srcStream = new BufferedInputStream(inFileStream);
		OutputStream destStream = this.stdin;
		new PipeStreamHandler(srcStream, destStream, true).start();
	}

	public void setOutputRedirect(OutputType fd, String writeFileName, boolean append) {
		try {
			if(fd == STDOUT_FILENO) {
				this.outFileStream = new FileOutputStream(writeFileName, append);
			} 
			else if(fd == STDERR_FILENO) {
				this.errFileStream = new FileOutputStream(writeFileName, append);
			}
			this.sBuilder.append(" " + fd);
			this.sBuilder.append(">");
			if(append) {
				this.sBuilder.append(">");
			}
			this.sBuilder.append(writeFileName);
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeFile(OutputType fd) {
		InputStream srcStream = null;
		OutputStream destStream = null;
		switch(fd) {
		case STDOUT_FILENO:
			if(this.outFileStream == null) {
				return;
			}
			srcStream = this.accessOutStream();
			destStream = new BufferedOutputStream(this.outFileStream);
			break;
		case STDERR_FILENO:
			if(this.errFileStream == null) {
				return;
			}
			srcStream = this.accessErrorStream();
			destStream = new BufferedOutputStream(this.errFileStream);
			break;
		}
		new PipeStreamHandler(srcStream, destStream, true).start();
	}

	@Override public void waitTermination() {
		try {
			this.retValue = this.proc.waitFor();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override public void kill() {
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
		catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} 
		catch (SecurityException e) {
			throw new RuntimeException(e);
		} 
		catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} 
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} 
		catch (IOException e) {
			throw new RuntimeException(e);
		} 
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void checkTermination() {
		this.retValue = this.proc.exitValue();
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
		return this.enableSyscallTrace;
	}
}

class MessageStreamHandler {
	private InputStream[] srcStreams;
	private OutputStream[] destStreams;
	private ByteArrayOutputStream messageBuffer;
	private PipeStreamHandler[] streamHandlers;

	public MessageStreamHandler(InputStream[] srcStreams, OutputStream destStream) {
		this.srcStreams = srcStreams;
		this.messageBuffer = new ByteArrayOutputStream();
		this.streamHandlers = new PipeStreamHandler[this.srcStreams.length];
		OutputStream[] tempStreams = {destStream, this.messageBuffer};
		this.destStreams = tempStreams;
	}

	public void showMessage() {
		boolean[] closeOutputs = {false, false};
		for(int i = 0; i < srcStreams.length; i++) {
			this.streamHandlers[i] = new PipeStreamHandler(this.srcStreams[i], this.destStreams, true, closeOutputs);
			this.streamHandlers[i].start();
		}
	}

	public String waitTermination() {
		for(int i = 0; i < srcStreams.length; i++) {
			try {
				this.streamHandlers[i].join();
			} 
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return this.messageBuffer.toString();
	}
}

// copied from http://blog.art-of-coding.eu/piping-between-processes/
class PipeStreamHandler extends Thread {
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

	public PipeStreamHandler(InputStream input, 
			OutputStream[] outputs, boolean closeInput, boolean[] closeOutputs) {
		this.input = input;
		this.outputs = new OutputStream[outputs.length];
		this.closeInput = closeInput;
		this.closeOutputs = closeOutputs;
		for(int i = 0; i < this.outputs.length; i++) {
			this.outputs[i] = (outputs[i] == null) ? new NullStream() : outputs[i];
		}
	}

	@Override public void run() {
		if(this.input == null) {
			return;
		}
		try {
			byte[] buffer = new byte[512];
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
		@Override public void write(int b) throws IOException {
			// do nothing
		}
		@Override public void close() {
			//do nothing
		}
	}
}

