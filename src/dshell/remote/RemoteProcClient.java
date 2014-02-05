package dshell.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import dshell.lib.PseudoProcess;
import dshell.lib.Task;
import dshell.lib.TaskOption;
import dshell.util.Utils;
import static dshell.lib.TaskOption.Behavior.printable;
import static dshell.lib.TaskOption.Behavior.throwable;
import static dshell.lib.TaskOption.Behavior.returnable;
import static dshell.lib.TaskOption.Behavior.server;

import static dshell.lib.TaskOption.RetType.TaskType;

public class RemoteProcClient extends PseudoProcess {	// TODO: multiple remote hosts
	private RemoteContext context;
	private TaskOption option;
	private ArrayList<ArrayList<String>> remoteCommandsList;
	private Task remoteTask = null;

	public RemoteProcClient(TaskOption option) {
		this.option = TaskOption.of(TaskType, returnable, server);
		this.option.setFlag(printable, option.is(printable));
		this.option.setFlag(throwable, option.is(throwable));
	}

	public void setArgumentList(ArrayList<String> argList, ArrayList<ArrayList<String>> remoteCommandsList) {
		this.remoteCommandsList = remoteCommandsList;
		super.setArgumentList(argList);
		String[] args = this.commandList.get(1).split(":");
		int port;
		if(args.length == 1) {
			port = DShellDaemon.defaultPort;
		}
		else if(args.length == 2) {
			port = Integer.parseInt(args[1]);
		}
		else {
			throw new RuntimeException("invalid argument: " + this.commandList.get(1));
		}
		try {
			this.context = new RemoteContext(new Socket(args[0], port));
			this.context.sendCommand(new CommandRequest(this.remoteCommandsList, this.option));
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override 
	public void mergeErrorToOut() {
	}
	@Override 
	public void setInputRedirect(String readFileName) {
	}
	@Override 
	public void setOutputRedirect(int fd, String writeFileName, boolean append) {
	}	// do nothing

	@Override 
	public void start() {
		final PipedOutputStream outReceiveStream = new PipedOutputStream();
		final PipedOutputStream errReceiveStream = new PipedOutputStream();
		this.stdin = new RedirToRemoteInputStream();
		try {
			this.stdout = new PipedInputStream(outReceiveStream);
			this.stderr = new PipedInputStream(errReceiveStream);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		this.context.sendStartRequest();
		if(this.isFirstProc) {
			try {
				this.accessInStream().close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		new Thread() {
			@Override
			public void run() {
				byte[] buffer = new byte[512];
				while(true) {
					int request = context.receiveRequest();
					int count = 0;
					if(context.matchRequest(request, RemoteContext.STREAM_REQ)) {
						int size = context.getStreamSize(request);
						int readSize = context.receiveStream(buffer, size);
						try {
							if(context.matchStreamType(request, RemoteContext.OUT_STREAM)) {
								outReceiveStream.write(buffer, 0, readSize);
							}
							else if(context.matchStreamType(request, RemoteContext.ERR_STREAM)) {
								errReceiveStream.write(buffer, 0, readSize);
							}
							else {
								Utils.fatal(1, "invalid stream type: " + request);
							}
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					}
					else if(context.matchRequest(request, RemoteContext.EOS_REQ)) {
						if(count == 2) {
							break;
						}
						count++;
					}
					else if(context.matchRequest(request, RemoteContext.RESULT_REQ)) {
						Task task = context.receiveTask();
						synchronized (errReceiveStream) {
							remoteTask = task;
						}
						break;
					}
					else {
						Utils.fatal(1, "invalid request: " + request);
					}
				}
			}
		}.start();
	}

	@Override 
	public void kill() {	//TOD: send force termination command
		this.waitTermination();
	}

	@Override 
	public void waitTermination() {
		while(true) {
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			synchronized (TaskType) {
				if(remoteTask != null) {
					break;
				}
			}
		}
		System.out.println(this.remoteTask.getOutMessage());
	}

	public Task getRemoteTask() {
		return this.remoteTask;
	}

	class RedirToRemoteInputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
		}	// do nothing. do not call it
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			context.sendStream(RemoteContext.IN_STREAM, b, len);
		}
		@Override
		public void close() {	//do nothing
			context.sendEndOfStream(RemoteContext.IN_STREAM);
		}
	}
}

