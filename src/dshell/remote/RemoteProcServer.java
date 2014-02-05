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

	private boolean isShutdown = false;
	
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
		new Thread() {
			@Override
			public void run() {
				byte[] buffer = new byte[512];
				while(true) {
					int request = context.receiveRequest();
					if(context.matchRequest(request, RemoteContext.STREAM_REQ)) {
						if(context.matchStreamType(request, RemoteContext.IN_STREAM)) {
							int size = context.getStreamSize(request);
							int readSize = context.receiveStream(buffer, size);
							try {
								inReceiveStream.write(buffer, 0, readSize);
							}
							catch (IOException e) {
								e.printStackTrace();
							}
						}
						else {
							Utils.fatal(1, "invalid stream type: " + request);
						}
					}
					else if(context.matchRequest(request, RemoteContext.EOS_REQ)) {
						if(context.matchStreamType(request, RemoteContext.IN_STREAM)) {
							try {
								inReceiveStream.close();
								synchronized (inReceiveStream) {
									isShutdown = true;
								}
								break;
							}
							catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					else {
						Utils.fatal(1, "invalid request: " + request);
					}
				}
			}
		}.start();
	}

	@Override
	public void kill() {	// TODO: support timeout
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
			synchronized (context) {
				if(this.isShutdown) {
					break;
				}
			}
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
