package dshell.internal.lib;

public class CommandContext {
	private final dshell.lang.InputStream stdin;
	private final dshell.lang.OutputStream stdout;
	private final dshell.lang.OutputStream stderr;
	private int exitStatus;

	public CommandContext(dshell.lang.InputStream stdin, dshell.lang.OutputStream stdout, dshell.lang.OutputStream stderr) {
		this.stdin = stdin;
		this.stdout = stdout;
		this.stderr = stderr;
		this.exitStatus = 0;
	}

	public dshell.lang.InputStream getStdin() {
		return this.stdin;
	}

	public dshell.lang.OutputStream getStdout() {
		return this.stdout;
	}

	public dshell.lang.OutputStream getStderr() {
		return this.stderr;
	}

	public int getExitStatus() {
		return this.exitStatus;
	}

	public void setExistStatus(int exitStatus) {
		this.exitStatus = exitStatus;
	}
}
