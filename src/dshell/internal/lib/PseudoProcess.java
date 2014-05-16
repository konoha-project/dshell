package dshell.internal.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import dshell.internal.lib.CommandArg.SubstitutedArg;

public abstract class PseudoProcess {
	public static final int STDOUT_FILENO = 1;
	public static final int STDERR_FILENO = 2;

	protected OutputStream stdin = null;
	protected InputStream stdout = null;
	protected InputStream stderr = null;

	protected StringBuilder cmdNameBuilder;
	protected ArrayList<String> commandList;
	protected StringBuilder sBuilder;

	protected boolean stdinIsDirty = false;
	protected boolean stdoutIsDirty = false;
	protected boolean stderrIsDirty = false;

	protected boolean isFirstProc = false;
	protected boolean isLastProc = false;

	protected int retValue = 0;

	public PseudoProcess() {
		this.cmdNameBuilder = new StringBuilder();
		this.commandList = new ArrayList<String>();
		this.sBuilder = new StringBuilder();
	}

	public void setArgumentList(ArrayList<CommandArg> argList) {
		int size = argList.size();
		for(int i = 0; i < size; i++) {
			CommandArg arg = argList.get(i);
			if(i != 0) {
				this.cmdNameBuilder.append(" ");
			}
			this.cmdNameBuilder.append(arg);
			this.addToCommandList(arg);
		}
	}

	protected final void addToCommandList(CommandArg arg) {
		if(arg.eq("")) {
			return;
		}
		if(arg instanceof SubstitutedArg) {
			this.commandList.addAll(((SubstitutedArg)arg).getValueList());
		}
		else {
			this.commandList.add(arg.toString());
		}
	}

	abstract public void mergeErrorToOut();
	abstract public void setInputRedirect(CommandArg readFileName);
	abstract public void setOutputRedirect(int fd, CommandArg writeFileName, boolean append);
	abstract public void start();
	abstract public void kill();
	abstract public void waitTermination();
	abstract public boolean checkTermination();

	public void pipe(PseudoProcess srcProc) {
		new PipeStreamHandler(srcProc.accessOutStream(), this.accessInStream(), true).start();
	}

	public void setFirstProcFlag(boolean isFirstProc) {
		this.isFirstProc = isFirstProc;
	}

	public void setLastProcFlag(boolean isLastProc) {
		this.isLastProc = isLastProc;
	}

	public OutputStream accessInStream() {
		if(!this.stdinIsDirty) {
			this.stdinIsDirty = true;
			return this.stdin;
		}
		return new PipeStreamHandler.NullOutputStream();
	}

	public InputStream accessOutStream() {
		if(!this.stdoutIsDirty) {
			this.stdoutIsDirty = true;
			return this.stdout;
		}
		return new PipeStreamHandler.NullInputStream();
	}

	public InputStream accessErrorStream() {
		if(!this.stderrIsDirty) {
			this.stderrIsDirty = true;
			return this.stderr;
		}
		return new PipeStreamHandler.NullInputStream();
	}

	public int getRet() {
		return this.retValue;
	}

	public String getCmdName() {
		return this.cmdNameBuilder.toString();
	}

	public boolean isTraced() {
		return false;
	}

	@Override public String toString() {
		return this.sBuilder.toString();
	}
}

class PipeStreamHandler extends Thread {
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
}