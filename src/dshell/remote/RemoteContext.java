package dshell.remote;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import dshell.lib.Task;

public class RemoteContext {
	// protocol definition
	// request header
	private final static int reqShiftWidth = 2;
	public final static int COMMAND_REQ  = 1;
	public final static int START_REQ    = 2;
	public final static int STREAM_REQ   = 3;
	public final static int EOS_REQ      = 4;
	public final static int SHUTDOWN_REQ = 5;
	public final static int RESULT_REQ   = 6;
	// optional argument for STREAM_REQ or EOS_REQ
	public final static int IN_STREAM    = 1;
	public final static int OUT_STREAM   = 2;
	public final static int ERR_STREAM   = 3;

	private final Socket socket;
	private ObjectInputStream socketInputStream = null;
	private ObjectOutputStream socketOutputStream = null;

	public RemoteContext(Socket socket) {
		this.socket = socket;
	}

	private void sendRequest(int request) {
		try {
			if(this.socketOutputStream == null) {
				this.socketOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
			}
			this.socketOutputStream.write(request);
			this.socketOutputStream.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendRequest(int request, int option) {
		this.sendRequest((request << reqShiftWidth) | option);
	}

	public int[] receiveRequest() {
		try {
			if(this.socketInputStream == null) {
				this.socketInputStream = new ObjectInputStream(this.socket.getInputStream());
			}
			int req =  this.socketInputStream.read();
			int mask = 0xfc;
			return new int[] {req >> reqShiftWidth, (req | mask) & ~mask};
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean receiveAndMatchRequest(int requestHeader) {
		int[] reqs = this.receiveRequest();
		return reqs[0] == requestHeader;
	}

	public synchronized void sendStartRequest() {
		this.sendRequest(START_REQ, 0);
	}

	public synchronized void sendShutdownRequest() {
		this.sendRequest(SHUTDOWN_REQ, 0);
	}

	public synchronized void sendCommand(CommandRequest commandReq) {
		try {
			this.sendRequest(COMMAND_REQ, 0);
			this.socketOutputStream.writeObject(commandReq);
			this.socketOutputStream.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public CommandRequest receiveCommand() {
		try {
			return (CommandRequest) this.socketInputStream.readObject();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public synchronized void sendTask(Task task) {
		try {
			this.sendRequest(RESULT_REQ, 0);
			this.socketOutputStream.writeObject(task);
			this.socketOutputStream.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Task receiveTask() {
		try {
			return (Task) this.socketInputStream.readObject();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public synchronized void sendStream(int streamType, byte[] buffer, int size) {
		try {
			this.sendRequest(STREAM_REQ, streamType);
			StreamRequest request = new StreamRequest(buffer, size);
			System.err.println(request.getBuffer());
			this.socketOutputStream.writeObject(request);
			this.socketOutputStream.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public StreamRequest receiveStream() {
		try {
			StreamRequest request = (StreamRequest) this.socketInputStream.readObject();
			System.out.println(request.getBuffer());
			return request;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public synchronized void sendEndOfStream(int streamType) {
		this.sendRequest(EOS_REQ, streamType);
	}

	public void closeSocket() {
		try {
			this.socket.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
