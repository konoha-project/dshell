package dshell.internal.remote;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;

import dshell.internal.lib.Utils;
import dshell.internal.process.CommandArg;
import dshell.internal.process.PseudoProcess;
import dshell.lang.Task;

public class RequestSender extends PseudoProcess {
	private final String requestString;
	private final boolean isBackground;
	private Process proc;
	private Thread resultHandler;
	private Task result;

	public RequestSender(ArrayList<ArrayList<CommandArg>> cmdsList, boolean isBackground) {
		this.requestString = CommandRequest.encodeToString(new CommandRequest(cmdsList, isBackground));
		this.isBackground = isBackground;
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
		if(this.commandList.size() == 2) {
			ProcessBuilder prcoBuilder;
			if(this.commandList.get(1).equals("localhost")) {
				prcoBuilder = new ProcessBuilder("dshell", "--receive", this.requestString);
			}
			else {
				String[] splittedString = this.commandList.get(1).toString().split(":");
				if(splittedString.length == 1) {
					prcoBuilder = new ProcessBuilder("ssh", this.commandList.get(1).toString(), "dshell", "--receive", this.requestString);
				}
				else if(splittedString.length == 2) {
					prcoBuilder = new ProcessBuilder("ssh", splittedString[0], "-p", splittedString[1], "dshell", "--receive", this.requestString);
				}
				else {
					Utils.fatal(1, "invalid argument: " + this.commandList.get(1));
					return;
				}
			}
			prcoBuilder.redirectError(Redirect.INHERIT);
			try {
				this.proc = prcoBuilder.start();
				if(this.isBackground) {
					return;
				}
				this.resultHandler = new Thread() {
					@Override public void run() {
						try {
							result = (Task) new ObjectInputStream(proc.getInputStream()).readObject();
						}
						catch (ClassNotFoundException e) {
							e.printStackTrace();
							Utils.fatal(1, "invalid class");
						}
						catch (IOException e) {
							e.printStackTrace();
							Utils.fatal(1, "IO problem");
						}
					}
				};
				this.resultHandler.start();
			}
			catch (IOException e) {
				e.printStackTrace();
				Utils.fatal(1, "IO problem");
			}
		}
	}

	@Override
	public void kill() {
	}

	@Override
	public void waitTermination() {
		if(!this.isBackground) {
			try {
				this.resultHandler.join();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean checkTermination() {
		return false;
	}

	public Task getRemoteTask() {
		return this.result;
	}
}
