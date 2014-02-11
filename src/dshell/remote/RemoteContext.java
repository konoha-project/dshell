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

	private void sendRequest(int request) throws IOException {
		if(this.socketOutputStream == null) {
			this.socketOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
		}
		this.socketOutputStream.write(request);
	}

	private void sendRequest(int request, int option) throws IOException {
		this.sendRequest((request << reqShiftWidth) | option);
	}

	public int[] receiveRequest() {
		try {
			if(this.socketInputStream == null) {
				this.socketInputStream = new ObjectInputStream(this.socket.getInputStream());
			}
			int req = this.socketInputStream.read();
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
		try {
			this.sendRequest(START_REQ, 0);
			this.socketOutputStream.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void sendShutdownRequest() {
		try {
			this.sendRequest(SHUTDOWN_REQ, 0);
			this.socketOutputStream.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
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

	public synchronized void sendStream(int streamType, int b) {
		try {
			this.sendRequest(STREAM_REQ, streamType);
			this.socketOutputStream.write(b);
			this.socketOutputStream.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int receiveStream() {
		try {
			return this.socketInputStream.read();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public synchronized void sendEndOfStream(int streamType) {
		try {
			this.sendRequest(EOS_REQ, streamType);
			this.socketOutputStream.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
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
