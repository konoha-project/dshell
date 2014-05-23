package dshell.internal.lib;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Calendar;

import dshell.lang.NativeException;

public class SubProc extends PseudoProcess {
	public final static int traceBackend_ltrace = 0;
	public static int traceBackendType = traceBackend_ltrace;

	private final static String logDirPath = "/tmp/dshell-trace-log";
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
			logFilePath = new String(logDirPath + "/" + createLogNameHeader() + ".log");
			new File(logDirPath).mkdir();
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
		catch(IOException e) {
			throw NativeException.wrapException(e);
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