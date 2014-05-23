package dshell.internal.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class PipeStreamHandler extends Thread {
	public final static int defaultBufferSize = 512;
	private final InputStream input;
	private final OutputStream[] outputs;
	private final boolean closeableInput;
	private final boolean[] closeableOutputs;

	public PipeStreamHandler(InputStream input, OutputStream output, boolean closeableStream) {
		this(input, new OutputStream[] {output}, closeableStream, new boolean[]{closeableStream});
	}

	public PipeStreamHandler(InputStream input, OutputStream[] outputs, boolean closeableInput, boolean[] closeableOutputs) {
		this.input = (input == null) ? new NullInputStream() : input;
		this.outputs = new OutputStream[outputs.length];
		this.closeableInput = closeableInput;
		this.closeableOutputs = closeableOutputs;
		for(int i = 0; i < this.outputs.length; i++) {
			this.outputs[i] = (outputs[i] == null) ? new NullOutputStream() : outputs[i];
		}
	}

	@Override
	public void run() {
		ArrayList<OutputStream> targetOutputList = new ArrayList<OutputStream>();
		targetOutputList.addAll(Arrays.asList(this.outputs));
		byte[] buffer = new byte[defaultBufferSize];
		int read = 0;
		try {
			while((read = this.input.read(buffer, 0, buffer.length)) > -1) {
				if(!this.writeToStreams(targetOutputList, buffer, read)) {
					break;
				}
			}
		}
		catch (IOException e) {
			if(RuntimeContext.getContext().isDebugMode()) {
				System.err.println("input problem");
				e.printStackTrace();
			}
		}
		this.closeInput();
		this.closeOutputs();
	}

	private boolean writeToStreams(ArrayList<OutputStream> targetOutputList, byte[] buffer, int readSize) {
		if(targetOutputList.size() == 0) {
			return false;
		}
		for(Iterator<OutputStream> iterator = targetOutputList.iterator(); iterator.hasNext(); ) {
			OutputStream output = iterator.next();
			try {
				output.write(buffer, 0, readSize);
			}
			catch(IOException e) {
				iterator.remove();
				if(RuntimeContext.getContext().isDebugMode()) {
					System.err.println("output problem");
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	private void closeInput() {
		if(!this.closeableInput) {
			return;
		}
		try {
			this.input.close();
		}
		catch (IOException e) {
			if(RuntimeContext.getContext().isDebugMode()) {
				System.err.println("close input problem");
				e.printStackTrace();
			}
		}
	}

	private void closeOutputs() {
		for(int i = 0; i < this.outputs.length; i++) {
			if(!this.closeableOutputs[i]) {
				continue;
			}
			try {
				this.outputs[i].close();
			}
			catch (IOException e) {
				if(RuntimeContext.getContext().isDebugMode()) {
					System.err.println("close output problem");
					e.printStackTrace();
				}
			}
		}
	}

	public static class NullInputStream extends InputStream {
		@Override
		public int read() throws IOException {
			return -1;
		}
		@Override
		public void close() {	//do nothing
		}
	}

	public static class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {	// do nothing
		}
		@Override
		public void close() {	//do nothing
		}
	}

	public static interface MessageStreamHandlerOp {
		public void startHandler();
		public String waitTermination();
		public ByteArrayOutputStream[] getEachBuffers();
	}

	public static class MessageStreamHandler implements MessageStreamHandlerOp {
		private InputStream[] srcStreams;
		private OutputStream consoleStream;
		private ByteArrayOutputStream messageBuffer;
		private ByteArrayOutputStream[] eachBuffers;
		private PipeStreamHandler[] streamHandlers;

		public MessageStreamHandler(InputStream[] srcStreams, OutputStream consoleStream) {
			this.srcStreams = srcStreams;
			this.messageBuffer = new ByteArrayOutputStream();
			this.streamHandlers = new PipeStreamHandler[this.srcStreams.length];
			this.consoleStream = consoleStream;
			this.eachBuffers = new ByteArrayOutputStream[this.srcStreams.length];
		}

		@Override
		public void startHandler() {
			boolean[] closeOutputs = {false, false, false};
			for(int i = 0; i < srcStreams.length; i++) {
				this.eachBuffers[i] = new ByteArrayOutputStream();
				OutputStream[] destStreams = new OutputStream[]{this.consoleStream, this.messageBuffer, this.eachBuffers[i]};
				this.streamHandlers[i] = new PipeStreamHandler(this.srcStreams[i], destStreams, true, closeOutputs);
				this.streamHandlers[i].start();
			}
		}

		@Override
		public String waitTermination() {
			for(PipeStreamHandler streamHandler : this.streamHandlers) {
				try {
					streamHandler.join();
				}
				catch(InterruptedException e) {
					e.printStackTrace();
					Utils.fatal(1, "interrupt problem");
				}
			}
			return Utils.removeNewLine(this.messageBuffer.toString());
		}

		@Override
		public ByteArrayOutputStream[] getEachBuffers() {
			return this.eachBuffers;
		}
	}

	public static class EmptyMessageStreamHandler implements MessageStreamHandlerOp {
		@Override
		public void startHandler() { // do nothing
		}

		@Override
		public String waitTermination() {
			return "";
		}

		@Override
		public ByteArrayOutputStream[] getEachBuffers() {
			return new ByteArrayOutputStream[0];
		}

		public static MessageStreamHandlerOp getHandler() {
			return Holder.HANDLER;
		}

		private static class Holder {
			private final static MessageStreamHandlerOp HANDLER = new EmptyMessageStreamHandler();
		}
	}
}
