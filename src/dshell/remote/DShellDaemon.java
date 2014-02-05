package dshell.remote;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import dshell.lib.Task;
import dshell.lib.TaskBuilder;
import dshell.lib.TaskOption;
import dshell.util.Utils;

public class DShellDaemon {
	public final static int defaultPort = 17777;
	private final int port;

	public DShellDaemon() { 
		this.port = defaultPort;
	}

	public DShellDaemon(int port) {
		this.port = port;
	}

	@SuppressWarnings("resource")
	public void waitConnection() {
		try {
			ServerSocket ss = new ServerSocket(this.port);
			while(true) {
				final Socket socket = ss.accept();
				new Thread() {
					@Override
					public void run() {
						System.out.println("create new Remote Context: " + socket.getInetAddress() +  " " + socket.getPort());
						RemoteContext context = new RemoteContext(socket);
						if(!context.receiveAndMatchRequest(RemoteContext.COMMAND_REQ)) {
							Utils.fatal(1, "require COMMAND_REQ");
						}
						CommandRequest commandReq = context.receiveCommand();
						System.out.println("receive command request:-->>");
						System.out.println(commandReq);
						TaskBuilder builder = new TaskBuilder(context, commandReq.getCommandsList(), commandReq.getOption());
						System.out.println("create taskbuilder:-->>");
						System.out.println(builder);
						if(!context.receiveAndMatchRequest(RemoteContext.START_REQ)) {
							Utils.fatal(1, "require START_REQ");
						}
						System.out.println("receive start request");
						Task task = (Task) builder.invoke();
						System.out.println("send result request:-->");
						System.out.println(task);
						context.sendTask(task);
//						context.closeSocket();
						System.out.println("close connection");
					}
				}.start();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}

class CommandRequest implements Serializable {
	private static final long serialVersionUID = -5370173768728461622L;
	private final ArrayList<ArrayList<String>> commandsList;
	private final TaskOption option;
//	private final ArrayList<String> flagList;

	public CommandRequest(ArrayList<ArrayList<String>> commandsList, TaskOption option) {
		this.commandsList = commandsList;
		this.option = option;
		//this.flagList = new ArrayList<String>();
		
	}

	public ArrayList<ArrayList<String>> getCommandsList() {
		return this.commandsList;
	}

	public TaskOption getOption() {
		return this.option;
	}

//	public String toString() {
//		return this.commandsList.toString() + "::: option " + this.option;
//	}
}