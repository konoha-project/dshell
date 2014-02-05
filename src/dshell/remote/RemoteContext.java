package dshell.remote;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import dshell.lib.Task;

public class RemoteContext {
	// protocol definition
	// request type
	private final static int reqShiftWidth = 16;
	public final static int COMMAND_REQ  = 1 << reqShiftWidth;
	public final static int START_REQ    = 2 << reqShiftWidth;
	public final static int STREAM_REQ   = 3 << reqShiftWidth;
	public final static int EOS_REQ      = 4 << reqShiftWidth;
	public final static int SHUTDOWN_REQ = 5 << reqShiftWidth;
	public final static int RESULT_REQ   = 6 << reqShiftWidth;
	// optional argument for STREAM_REQ
	private final static int typeShiftWidth = 10;
	public final static int IN_STREAM  = 0 << typeShiftWidth;
	public final static int OUT_STREAM = 1 << typeShiftWidth;
	public final static int ERR_STREAM = 2 << typeShiftWidth;

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
			this.socketOutputStream.writeInt(request);
			this.socketOutputStream.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int receiveRequest() {
		try {
			if(this.socketInputStream == null) {
				this.socketInputStream = new ObjectInputStream(this.socket.getInputStream());
			}
			return this.socketInputStream.readInt();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public boolean matchRequest(int request, int requestDef) {
		return (request >> reqShiftWidth) == (requestDef >> reqShiftWidth);
	}

	public boolean receiveAndMatchRequest(int requestDef) {
		int request = this.receiveRequest();
		return this.matchRequest(request, requestDef);
	}

	public boolean matchStreamType(int request, int streamType) {
		request <<= reqShiftWidth;
		request >>= reqShiftWidth;
		streamType <<= reqShiftWidth;
		streamType >>= reqShiftWidth;
		return (request >> typeShiftWidth) == (streamType >> typeShiftWidth);
	}

	public int getStreamSize(int request) {	//FIXME
		int mask = 0xFFFFFC00;
		request |= mask;
		return request & ~mask;
	}

	public synchronized void sendStartRequest() {
		this.sendRequest(START_REQ);
	}

	public synchronized void sendShutdownRequest() {
		this.sendRequest(SHUTDOWN_REQ);
	}

	public synchronized void sendCommand(CommandRequest commandReq) {
		try {
			this.sendRequest(COMMAND_REQ);
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
			this.sendRequest(RESULT_REQ);
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
			this.sendRequest(STREAM_REQ | streamType | size);
			this.socketOutputStream.write(buffer, 0, size);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int receiveStream(byte[] buffer, int size) {
		try {
			return this.socketInputStream.read(buffer, 0, size);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public synchronized void sendEndOfStream(int streamType) {
		this.sendRequest(EOS_REQ | streamType);
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
