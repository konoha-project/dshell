package dshell.internal.lib;

import dshell.internal.process.CommandArg;
import dshell.internal.process.PseudoProcess;

public class CommandRunner extends PseudoProcess {
	private final ExecutableAsCommand executor;
	private CommandContext context;
	private Thread runner;
	private boolean isTerminated;

	public CommandRunner(ExecutableAsCommand executor) {
		this.executor = executor;
		this.isTerminated = false;
	}

	@Override
	public void mergeErrorToOut() {
	}

	@Override
	public void setInputRedirect(CommandArg readFileName) {
	}

	@Override
	public void setOutputRedirect(int fd, CommandArg writeFileName, boolean append) {
	}

	@Override
	public void start() {
		this.context = new CommandContext(StreamUtils.createStdin(), StreamUtils.createStdout(), StreamUtils.createStderr());
		this.runner = new Thread() {
			@Override public void run() {
				executor.execute(context, commandList);
			}
		};
		this.runner.start();
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
