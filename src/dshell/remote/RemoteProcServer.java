package dshell.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import dshell.lib.PseudoProcess;
import dshell.util.Utils;

public class RemoteProcServer extends PseudoProcess {
	private final RemoteContext context;
	public final OutputStream outStream;
	public final OutputStream errStream;
	
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
		this.stdout = new InputStream() {
			boolean close = false;
			@Override
			public int read() throws IOException {
				int[] reqs = context.receiveRequest();
				int request = reqs[0];
				int option = reqs[1];
				if(request == RemoteContext.STREAM_REQ) {
					if(option == RemoteContext.IN_STREAM && !close) {
						return context.receiveStream();
					}
					else {
						Utils.fatal(1, "invalid stream type: " + option);
					}
				}
				else if(request == RemoteContext.EOS_REQ) {
					if(option == RemoteContext.IN_STREAM) {
						close = true;
						return -1;
					}
					else {
						Utils.fatal(1, "invalid stream type: " + option);
					}
				}
				else {
					Utils.fatal(1, "invalid request type: " + request);
				}
				return -1;
			}
		};
	}

	@Override
	public void kill() {	// TODO: support timeout
	}

	@Override
	public void waitTermination() {
	}

	@Override
	public boolean checkTermination() {
		return true;	//FIXME
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
			context.sendStream(this.streamType, b);
		}
		@Override
		public void close() {	//do nothing
			context.sendEndOfStream(streamType);
		}
	}
}
