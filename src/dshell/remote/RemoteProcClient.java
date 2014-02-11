package dshell.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
	private Thread requestHandler;

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
		this.stdin = new RedirToRemoteInputStream();
		if(this.option.supportStdoutHandler()) {
			
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
		this.requestHandler = new Thread() {
			@Override
			public void run() {
				while(true) {
					int[] reqs = context.receiveRequest();
					int request = reqs[0];
					int option = reqs[1];
					if(request == RemoteContext.STREAM_REQ) {
						if(option == RemoteContext.OUT_STREAM) {
							System.out.write(context.receiveStream());
						}
						else if(option == RemoteContext.ERR_STREAM) {
							System.err.write(context.receiveStream());
						}
						else {
							Utils.fatal(1, "invalid stream type: " + option);
						}
					}
					else if(request == RemoteContext.EOS_REQ) {
					}
					else if(request == RemoteContext.RESULT_REQ) {
						remoteTask = context.receiveTask();
						break;
					}
					else {
						break;
					}
				}
			}
		};
		this.requestHandler.start();
	}

	@Override 
	public void kill() {	//TOD: send force termination command
		this.waitTermination();
	}

	@Override 
	public void waitTermination() {
		try {
			this.requestHandler.join();
			this.context.closeSocket();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean checkTermination() {
		return true;	//FIXME
	}

	public Task getRemoteTask() {
		return this.remoteTask;
	}

	class RedirToRemoteInputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			context.sendStream(RemoteContext.IN_STREAM, b);
		}	
		@Override
		public void close() {	//do nothing
			context.sendEndOfStream(RemoteContext.IN_STREAM);
		}
	}

	class RemoteInputStream extends InputStream {
		private final LinkedBlockingQueue<StreamRequest> targetQueue;
		private boolean closed = false;
		private int currentIndex;
		private int currentBufferSize;
		private byte[] currentBuffer = null;

		public RemoteInputStream(LinkedBlockingQueue<StreamRequest> targetQueue) {
			this.targetQueue = targetQueue;
		}

		@Override
		public int read() throws IOException {
			if(this.closed) {
				return -1;
			}
			
			return -1;
		}
		@Override
		public void close() {	//do nothing
			this.closed = true;
		}
	}

	class StreamRequest implements Serializable {
		public static final int defaulrBufferSize = 32;
		private static final long serialVersionUID = -9141655789457233677L;
		private final byte[] buffer;

		public StreamRequest() {
			byte[] buffer = new byte[] {0};
			this.buffer = buffer;
		}

		public StreamRequest(byte[] buffer) {
			int size = buffer.length;
			this.buffer = new byte[size];
			for(int i = 0; i < size; i++) {
				this.buffer[i] = buffer[i];
			}
		}

		public byte[] getBuffer() {
			return this.buffer;
		}
	}

	class EndOfStreamRequest extends StreamRequest {
		private static final long serialVersionUID = 1758719870593483917L;
		public EndOfStreamRequest() {
			super();
		}
	}
}

