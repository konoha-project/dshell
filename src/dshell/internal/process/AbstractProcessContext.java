package dshell.internal.process;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import dshell.internal.lib.Utils;
import dshell.lang.GenericArray;

/**
 * represents process.
 * @author skgchxngsxyz-opensuse
 *
 */
public abstract class AbstractProcessContext {
	public static final int STDOUT_FILENO = 1;
	public static final int STDERR_FILENO = 2;

	/**
	 * reference of standard input of created process.
	 */
	protected OutputStream stdin = null;

	/**
	 * reference of standard output of created process.
	 */
	protected InputStream stdout = null;

	/**
	 * reference of standard error of created process.
	 */
	protected InputStream stderr = null;

	/**
	 * also contains command path. actually, it is LinkedList.
	 */
	protected final List<String> argList;

	/**
	 * if true, standard input has already used.
	 */
	protected boolean stdinIsDirty = false;

	/**
	 * if true, standard output has already used.
	 */
	protected boolean stdoutIsDirty = false;

	/**
	 * if true, standard error has already used.
	 */
	protected boolean stderrIsDirty = false;

	/**
	 * if true, this context behaves as first process.
	 */
	protected boolean isFirstProc = false;

	/**
	 * if true, this context behaves as last process.
	 */
	protected boolean isLastProc = false;

	/**
	 * exit status of process launched by this context.
	 */
	protected int retValue = 0;

	protected AbstractProcessContext(String commandPath) {
		this.argList = new LinkedList<>();
		this.argList.add(commandPath);
	}

	/**
	 * add command argument.
	 * @param arg
	 * - argument string.
	 * @return
	 * - this.
	 */
	public AbstractProcessContext addArg(String arg) {
		this.argList.add(Utils.resolveHome(arg));
		return this;
	}

	/**
	 * add arguments.
	 * @param argArray
	 * - must be string array.
	 * @return
	 * - this.
	 */
	public AbstractProcessContext addArg(GenericArray argArray) {
		long size = argArray.size();
		for(long i = 0; i < size; i++) {
			this.argList.add((String) argArray.get(i));
		}
		return this;
	}

	public abstract AbstractProcessContext mergeErrorToOut();
	public abstract AbstractProcessContext setInputRedirect(String readFileName);
	public abstract AbstractProcessContext setOutputRedirect(int fd, String writeFileName, boolean append);

	/**
	 * create and start new process.
	 * must call it only once.
	 * @return
	 * - this.
	 */
	public abstract AbstractProcessContext start();

	/**
	 * kill created prcoess.
	 */
	public abstract void kill();

	public abstract void waitTermination();

	/**
	 * 
	 * @return
	 * return true, if creted process has already terminated.
	 */
	public abstract boolean checkTermination();

	/**
	 * redirect srcContex's standard output to this context's standard input.
	 * @param srcConext
	 */
	public void pipe(AbstractProcessContext srcConext) {
		new PipeStreamHandler(srcConext.accessOutStream(), this.accessInStream(), true).start();
	}

	public void setAsFirstProc(boolean isFirstProc) {
		this.isFirstProc = isFirstProc;
	}

	public void setAsLastProc(boolean isLastProc) {
		this.isLastProc = isLastProc;
	}

	/**
	 * get standard input of created process.
	 * @return
	 * - return NullOutputStream, if cannot access.
	 */
	public OutputStream accessInStream() {
		if(!this.stdinIsDirty) {
			this.stdinIsDirty = true;
			return this.stdin;
		}
		return new PipeStreamHandler.NullOutputStream();
	}

	/**
	 * get standard output of created process.
	 * @return
	 * - return NullInoutStream, if cannot access.
	 */
	public InputStream accessOutStream() {
		if(!this.stdoutIsDirty) {
			this.stdoutIsDirty = true;
			return this.stdout;
		}
		return new PipeStreamHandler.NullInputStream();
	}

	/**
	 * get standard error of created process.
	 * @return
	 * - return NullInoutStream, if cannot access.
	 */
	public InputStream accessErrorStream() {
		if(!this.stderrIsDirty) {
			this.stderrIsDirty = true;
			return this.stderr;
		}
		return new PipeStreamHandler.NullInputStream();
	}

	/**
	 * get exit status of created process.
	 * @return
	 */
	public int getRet() {
		return this.retValue;
	}

	public String getCmdName() {	// FIXME:
		return "FIXME";
	}

	/**
	 * enable system call trace.
	 * @return
	 * - this.
	 */
	public AbstractProcessContext enableTrace() {
		return this;
	}

	/**
	 * 
	 * @return
	 * - if true, system call trace is enabled.
	 */
	public boolean isTraced() {
		return false;
	}
}
