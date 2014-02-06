package dshell.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;

import dshell.lib.PseudoProcess;
import dshell.util.Utils;

public class RemoteProcServer extends PseudoProcess {
	private final RemoteContext context;
	public final OutputStream outStream;
	public final OutputStream errStream;
	private Thread requestHandler;
	
	public RemoteProcServer(RemoteContext context) {
		this.context = context;
		this.outStream = new RemoteOutputStream(RemoteContext.OUT_STREAM);
		this.errStream = new RemoteOutputStream(RemoteContext.ERR_STREAM);
	}

	@Override
	public void mergeErrorToOut() {	// do nothing
	}
	@Override
	public void setInputRedirect(String readFileName) {	// TODO: support input redirect
	}
	@Override
	public void setOutputRedirect(int fd, String writeFileName, boolean append) {
	}	// do nothing
	@Override
	public void setArgumentList(ArrayList<String> argList) { // do nothing
	}

	@Override
	public void start() {
		final PipedOutputStream inReceiveStream = new PipedOutputStream();
		try {
			this.stdout = new PipedInputStream(inReceiveStream);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		this.requestHandler = new Thread() {
			@Override
			public void run() {
				while(true) {
					int[] reqs = context.receiveRequest();
					int request = reqs[0];
					int option = reqs[1];
					if(request == RemoteContext.STREAM_REQ) {
						if(option == RemoteContext.IN_STREAM) {
							StreamRequest streamReq = context.receiveStream();
							byte[] buffer = streamReq.getBuffer();
							try {
								inReceiveStream.write(buffer, 0, buffer.length);
							}
							catch (IOException e) {
								e.printStackTrace();
							}
						}
						else {
							Utils.fatal(1, "invalid stream type: " + option);
						}
					}
					else if(request == RemoteContext.EOS_REQ) {
						if(option == RemoteContext.IN_STREAM) {
							try {
								System.out.println("receive End OF InputStream");
								inReceiveStream.close();
							}
							catch (IOException e) {
								e.printStackTrace();
							}
							break;
						}
					}
				}
			}
		};
		this.requestHandler.start();
	}

	@Override
	public void kill() {	// TODO: support timeout
	}

	@Override
	public void waitTermination() {
		try {
			this.requestHandler.join();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	class RemoteOutputStream extends OutputStream {
		private final int streamType;
		public RemoteOutputStream(int streamType) {
			if(streamType != RemoteContext.OUT_STREAM && streamType != RemoteContext.ERR_STREAM) {
				throw new RuntimeException("invalid stream tyep: " + streamType);
			}
			this.streamType = streamType;
		}
		@Override
		public void write(int b) throws IOException {
			throw new RuntimeException();
		}	// do nothing. do not call it
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			context.sendStream(this.streamType, b, len);
		}
		@Override
		public void close() {	//do nothing
			context.sendEndOfStream(streamType);
		}
	}
}
