package dshell.internal.lib;

public class CommandContext {
	private final StreamUtils.InputStream stdin;
	private final StreamUtils.OutputStream stdout;
	private final StreamUtils.OutputStream stderr;
	private int exitStatus;

	public CommandContext(StreamUtils.InputStream stdin, StreamUtils.OutputStream stdout, StreamUtils.OutputStream stderr) {
		this.stdin = stdin;
		this.stdout = stdout;
		this.stderr = stderr;
		this.exitStatus = 0;
	}

	public StreamUtils.InputStream getStdin() {
		return this.stdin;
	}

	public StreamUtils.OutputStream getStdout() {
		return this.stdout;
	}

	public StreamUtils.OutputStream getStderr() {
		return this.stderr;
	}

	public int getExitStatus() {
		return this.exitStatus;
	}

	public void setExistStatus(int exitStatus) {
		this.exitStatus = exitStatus;
	}
}
