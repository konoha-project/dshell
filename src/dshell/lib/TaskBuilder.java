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
import dshell.util.Utils;

public class TaskBuilder {
	private int OptionFlag;
	private int retType;
	private PseudoProcess[] Processes;
	private long timeout = -1;
	private StringBuilder sBuilder;

	public MessageStreamHandler stdoutHandler;
	public MessageStreamHandler stderrHandler;

	public TaskBuilder(ArrayList<ArrayList<String>> cmdsList, int option, int retType) {
		this.OptionFlag = option;
		this.retType = retType;
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
		switch(this.retType) {
		case Utils.VoidType: this.sBuilder.append("VoidType"); break;
		case Utils.IntType: this.sBuilder.append("IntType"); break;
		case Utils.BooleanType: this.sBuilder.append("BooleanType"); break;
		case Utils.StringType: this.sBuilder.append("StringType"); break;
		case Utils.TaskType: this.sBuilder.append("TaskType"); break;
		}
		if(Utils.is(this.OptionFlag, Utils.returnable)) {
			this.sBuilder.append("|returnable");
		}
		if(Utils.is(this.OptionFlag, Utils.printable)) {
			this.sBuilder.append("|printable");
		}
		if(Utils.is(this.OptionFlag, Utils.throwable)) {
			this.sBuilder.append("|throwable");
		}
		if(Utils.is(this.OptionFlag, Utils.background)) {
			this.sBuilder.append("|background");
		}
		if(Utils.is(this.OptionFlag, Utils.inference)) {
			this.sBuilder.append("|inference");
		}
		this.sBuilder.append(">");
	}

	public Object invoke() {
		Task task = new Task(this);
		if(Utils.is(this.OptionFlag, Utils.background)) {
			return (this.retType == Utils.TaskType) && Utils.is(this.OptionFlag, Utils.returnable) ? task : null;
		}
		task.join();
		if(Utils.is(this.OptionFlag, Utils.returnable)) {
			if(this.retType == Utils.StringType) {
				return task.getOutMessage();
			}
			else if(this.retType == Utils.BooleanType) {
				return new Boolean(task.getExitStatus() == 0);
			}
			else if(this.retType == Utils.IntType){
				return new Integer(task.getExitStatus());
			}
			else if(this.retType == Utils.TaskType) {
				return task;
			}
		}
		return null;
	}

	public PseudoProcess[] getProcesses() {
		return this.Processes;
	}

	public int getOptionFlag() {
		return this.OptionFlag;
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
				this.OptionFlag = Utils.setFlag(this.OptionFlag, Utils.background, true);
				continue;
			}
			else if(currentCmds.get(0).equals(DShellGrammar.errorAction_raise)) {
				this.OptionFlag = Utils.setFlag(this.OptionFlag, Utils.throwable, true);
				continue;
			}
			else if(currentCmds.get(0).equals(DShellGrammar.errorAction_trace)) {
				this.OptionFlag = Utils.setFlag(this.OptionFlag, Utils.throwable, true);
				this.OptionFlag = Utils.setFlag(this.OptionFlag, Utils.inference, true);
				enableTrace = checkTraceRequirements();
				continue;
			}
			newCmdsBuffer.add(currentCmds);
		}
		if(Utils.is(this.OptionFlag, Utils.throwable)) {
			this.OptionFlag = Utils.setFlag(this.OptionFlag, Utils.inference, enableTrace);
		}
		return newCmdsBuffer;
	}

	private PseudoProcess[] createProcs(ArrayList<ArrayList<String>> cmdsList) {
		boolean enableSyscallTrace = Utils.is(this.OptionFlag, Utils.inference);
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
				prevProc.setOutputRedirect(SubProc.STDOUT_FILENO, currentCmds.get(1), false);
			}	
			else if(cmdSymbol.equals("1>>") || cmdSymbol.equals(">>")) {
				prevProc.setOutputRedirect(SubProc.STDOUT_FILENO, currentCmds.get(1), true);
			}
			else if(cmdSymbol.equals("2>")) {
				prevProc.setOutputRedirect(SubProc.STDERR_FILENO, currentCmds.get(1), false);
			}
			else if(cmdSymbol.equals("2>>")) {
				prevProc.setOutputRedirect(SubProc.STDERR_FILENO, currentCmds.get(1), true);
			}
			else if(cmdSymbol.equals("&>") || cmdSymbol.equals(">&")) {
				prevProc.setOutputRedirect(SubProc.STDOUT_FILENO, currentCmds.get(1), false);
				prevProc.setMergeType(SubProc.mergeErrorToOut);
			}
			else if(cmdSymbol.equals("&>>")) {
				prevProc.setOutputRedirect(SubProc.STDOUT_FILENO, currentCmds.get(1), true);
				prevProc.setMergeType(SubProc.mergeErrorToOut);
			}
			else if(cmdSymbol.equals(">&1") || cmdSymbol.equals("1>&1") || cmdSymbol.equals("2>&2")) {
				// do nothing
			}
			else if(cmdSymbol.equals("1>&2")) {
				prevProc.setMergeType(SubProc.mergeOutToError);
			}
			else if(cmdSymbol.equals("2>&1")) {
				prevProc.setMergeType(SubProc.mergeErrorToOut);
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
		int option = Utils.printable;
		new TaskBuilder(cmdsList, option, Utils.VoidType).invoke();
	}

	public static int ExecCommandIntJS(ArrayList<ArrayList<String>> cmdsList) {
		int option = Utils.printable | Utils.returnable;
		return ((Integer)new TaskBuilder(cmdsList, option, Utils.IntType).invoke()).intValue();
	}

	public static boolean ExecCommandBoolJS(ArrayList<ArrayList<String>> cmdsList) {
		int option = Utils.printable | Utils.returnable;
		return ((Boolean)new TaskBuilder(cmdsList, option, Utils.BooleanType).invoke()).booleanValue();
	}

	public static String ExecCommandStringJS(ArrayList<ArrayList<String>> cmdsList) {
		int option = Utils.returnable;
		return (String)new TaskBuilder(cmdsList, option, Utils.StringType).invoke();
	}

	public static Task ExecCommandTaskJS(ArrayList<ArrayList<String>> cmdsList) {
		int option = Utils.printable | Utils.returnable;
		return (Task)new TaskBuilder(cmdsList, option, Utils.TaskType).invoke();
	}

	// called by ModifiedJavaByteCodeGenerator#VisitCommandNode
	public static void ExecCommandVoid(String[][] cmds) {
		int option = Utils.printable;
		new TaskBuilder(toCmdsList(cmds), option, Utils.VoidType).invoke();
	}

	public static int ExecCommandInt(String[][] cmds) {
		int option = Utils.printable | Utils.returnable;
		return ((Integer)new TaskBuilder(toCmdsList(cmds), option, Utils.IntType).invoke()).intValue();
	}

	public static boolean ExecCommandBool(String[][] cmds) {
		int option = Utils.printable | Utils.returnable;
		return ((Boolean)new TaskBuilder(toCmdsList(cmds), option, Utils.BooleanType).invoke()).booleanValue();
	}

	public static String ExecCommandString(String[][] cmds) {
		int option = Utils.returnable;
		return (String)new TaskBuilder(toCmdsList(cmds), option, Utils.StringType).invoke();
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

class PseudoProcess {
	public final static int mergeErrorToOut = 0;
	public final static int mergeOutToError = 1;

	protected OutputStream stdin = null;
	protected InputStream stdout = null;
	protected InputStream stderr = null;

	protected StringBuilder cmdNameBuilder;
	protected ArrayList<String> commandList;
	protected StringBuilder sBuilder;

	protected boolean stdoutIsDirty = false;
	protected boolean stderrIsDirty = false;

	protected int mergeType = -1;
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

	public void setMergeType(int mergeType) {
		this.mergeType = mergeType;
		if(this.mergeType == SubProc.mergeErrorToOut) {
			this.sBuilder.append(" 2>&1");
		}
		else if(this.mergeType == SubProc.mergeOutToError) {
			this.sBuilder.append(" 1>&2");
		}
	}

	public void start() {
	}

	public void pipe(PseudoProcess srcProc) {
		new PipeStreamHandler(srcProc.accessOutStream(), this.stdin, true).start();
	}

	public void kill() {
	}

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
	public final static int traceBackend_ltrace      = 0;
	public static int traceBackendType = traceBackend_ltrace;

	private final static String logdirPath = "/tmp/dshell-trace-log";
	private static int logId = 0;
	
	public final static int STDOUT_FILENO = 1;
	public final static int STDERR_FILENO = 2;

	private Process proc;
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
			if(this.mergeType == SubProc.mergeErrorToOut || this.mergeType == SubProc.mergeOutToError) {
				procBuilder.redirectErrorStream(true);
			}
			this.proc = procBuilder.start();
			this.stdin = this.proc.getOutputStream();
			if(this.mergeType == SubProc.mergeOutToError) {
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

	public void setOutputRedirect(int fd, String writeFileName, boolean append) {
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

	private void writeFile(int fd) {
		InputStream srcStream;
		OutputStream destStream;
		if(fd == STDOUT_FILENO) {
			if(this.outFileStream == null) {
				return;
			}
			srcStream = this.accessOutStream();
			destStream = new BufferedOutputStream(this.outFileStream);
		}
		else if(fd == STDERR_FILENO) {
			if(this.errFileStream == null) {
				return;
			}
			srcStream = this.accessErrorStream();
			destStream = new BufferedOutputStream(this.errFileStream);
		}
		else {
			throw new RuntimeException("invalid file descriptor");
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
			this.outputs[i] = outputs[i] == null ? new NullStream() : outputs[i];
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

