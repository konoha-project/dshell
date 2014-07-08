package dshell.internal.lib;

import dshell.internal.process.AbstractProcessContext;

public class CommandRunner extends AbstractProcessContext {
	private final ExecutableAsCommand executor;
	private CommandContext context;
	private Thread runner;
	private boolean isTerminated;

	public CommandRunner(String commandName, ExecutableAsCommand executor) {
		super(commandName);
		this.executor = executor;
		this.isTerminated = false;
	}

	@Override
	public AbstractProcessContext mergeErrorToOut() {
		return this;
	}

	@Override
	public AbstractProcessContext setInputRedirect(String readFileName) {
		return this;
	}

	@Override
	public AbstractProcessContext setOutputRedirect(int fd, String writeFileName, boolean append) {
		return this;
	}

	@Override
	public AbstractProcessContext start() {
		this.context = new CommandContext(dshell.lang.InputStream.createStdin(), dshell.lang.OutputStream.createStdout(), dshell.lang.OutputStream.createStderr());
		this.runner = new Thread() {
			@Override public void run() {
				executor.execute(context, argList);
			}
		};
		this.runner.start();
		return this;
	}

	@Override
	public void kill() {	// do nothing
	}

	@Override
	public void waitTermination() {
		try {
			this.runner.join();
			this.retValue = this.context.getExitStatus();
			this.isTerminated = true;
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean checkTermination() {
		return this.isTerminated;
	}
}
