package dshell.lib;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;

import dshell.util.Utils;

import static dshell.lib.TaskOption.Behavior.returnable;
import static dshell.lib.TaskOption.Behavior.printable ;
import static dshell.lib.TaskOption.Behavior.throwable ;
import static dshell.lib.TaskOption.Behavior.background;
import static dshell.lib.TaskOption.Behavior.receivable;

import static dshell.lib.TaskOption.RetType.VoidType   ;
import static dshell.lib.TaskOption.RetType.TaskType   ;

public class RequestSender extends PseudoProcess {
	private final CommandRequest request;
	private final boolean isBackground;
	private Process proc;
	private Thread resultHandler;
	private Task result;

	public RequestSender(ArrayList<ArrayList<String>> cmdsList, boolean isBackground) {
		this.request = new CommandRequest(cmdsList, isBackground);
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
		if(this.commandList.size() == 2 && this.commandList.get(1).equals("localhost")) {
			ProcessBuilder prcoBuilder  = new ProcessBuilder("/usr/local/bin/dshell", "--receive");
			prcoBuilder.redirectError(Redirect.INHERIT);
			try {
				this.proc = prcoBuilder.start();
				new ObjectOutputStream(proc.getOutputStream()).writeObject(this.request);
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

class CommandRequest implements Serializable {
	private static final long serialVersionUID = 2907070450718884160L;
	private final ArrayList<ArrayList<String>> cmdsList;
	private final TaskOption option;

	public CommandRequest(ArrayList<ArrayList<String>> cmdsList, boolean isBackground) {
		this.cmdsList = cmdsList;
		if(isBackground) {
			this.option = TaskOption.of(VoidType, background, printable, receivable);
		}
		else {
			this.option = TaskOption.of(TaskType, returnable, throwable, receivable);
		}
	}

	public ArrayList<ArrayList<String>> getCmdsList() {
		return this.cmdsList;
	}

	public TaskOption getOption() {
		return this.option;
	}
}
