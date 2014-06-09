package dshell.internal.process;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import dshell.internal.process.CommandArg.SubstitutedArg;
import dshell.internal.process.PipeStreamHandler.NullInputStream;
import dshell.internal.process.PipeStreamHandler.NullOutputStream;

public abstract class PseudoProcess {
	public static final int STDOUT_FILENO = 1;
	public static final int STDERR_FILENO = 2;

	protected OutputStream stdin = null;
	protected InputStream stdout = null;
	protected InputStream stderr = null;

	protected StringBuilder cmdNameBuilder;
	protected ArrayList<String> commandList;
	protected StringBuilder sBuilder;

	protected boolean stdinIsDirty = false;
	protected boolean stdoutIsDirty = false;
	protected boolean stderrIsDirty = false;

	protected boolean isFirstProc = false;
	protected boolean isLastProc = false;

	protected int retValue = 0;

	public PseudoProcess() {
		this.cmdNameBuilder = new StringBuilder();
		this.commandList = new ArrayList<String>();
		this.sBuilder = new StringBuilder();
	}

	public void setArgumentList(ArrayList<CommandArg> argList) {
		int size = argList.size();
		for(int i = 0; i < size; i++) {
			CommandArg arg = argList.get(i);
			if(i != 0) {
				this.cmdNameBuilder.append(" ");
			}
			this.cmdNameBuilder.append(arg);
			this.addToCommandList(arg);
		}
	}

	protected final void addToCommandList(CommandArg arg) {
		if(arg.eq("")) {
			return;
		}
		if(arg instanceof SubstitutedArg) {
			this.commandList.addAll(((SubstitutedArg)arg).getValueList());
		}
		else {
			this.commandList.add(arg.toString());
		}
	}

	abstract public void mergeErrorToOut();
	abstract public void setInputRedirect(CommandArg readFileName);
	abstract public void setOutputRedirect(int fd, CommandArg writeFileName, boolean append);
	abstract public void start();
	abstract public void kill();
	abstract public void waitTermination();
	abstract public boolean checkTermination();

	public void pipe(PseudoProcess srcProc) {
		new PipeStreamHandler(srcProc.accessOutStream(), this.accessInStream(), true).start();
	}

	public void setFirstProcFlag(boolean isFirstProc) {
		this.isFirstProc = isFirstProc;
	}

	public void setLastProcFlag(boolean isLastProc) {
		this.isLastProc = isLastProc;
	}

	public OutputStream accessInStream() {
		if(!this.stdinIsDirty) {
			this.stdinIsDirty = true;
			return this.stdin;
		}
		return new PipeStreamHandler.NullOutputStream();
	}

	public InputStream accessOutStream() {
		if(!this.stdoutIsDirty) {
			this.stdoutIsDirty = true;
			return this.stdout;
		}
		return new PipeStreamHandler.NullInputStream();
	}

	public InputStream accessErrorStream() {
		if(!this.stderrIsDirty) {
			this.stderrIsDirty = true;
			return this.stderr;
		}
		return new PipeStreamHandler.NullInputStream();
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
