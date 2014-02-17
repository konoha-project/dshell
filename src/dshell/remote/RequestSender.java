package dshell.remote;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;

import dshell.lib.PseudoProcess;
import dshell.lib.Task;
import dshell.util.Utils;

public class RequestSender extends PseudoProcess {
	private final String requestString;
	private final boolean isBackground;
	private Process proc;
	private Thread resultHandler;
	private Task result;

	public RequestSender(ArrayList<ArrayList<String>> cmdsList, boolean isBackground) {
		this.requestString =CommandRequest.encodeToString(new CommandRequest(cmdsList, isBackground));
		this.isBackground = isBackground;
	}

	@Override
	public void mergeErrorToOut() {
	}
	@Override
	public void setInputRedirect(String readFileName) {
	}
	@Override
	public void setOutputRedirect(int fd, String writeFileName, boolean append) {
	}

	@Override
	public void start() {
		if(this.commandList.size() == 2) {
			ProcessBuilder prcoBuilder;
			if(this.commandList.get(1).equals("localhost")) {
				prcoBuilder = new ProcessBuilder("dshell", "--receive", this.requestString);
			}
			else {
				prcoBuilder = new ProcessBuilder("ssh", this.commandList.get(1), "dshell", "--receive", this.requestString);
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
			System.out.println(this.result.getOutMessage());
		}
	}

	@Override
	public boolean checkTermination() {
		return false;
	}
}
