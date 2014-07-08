package dshell.lang;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import dshell.annotation.Shared;
import dshell.annotation.SharedClass;

/**
 * D-Shell output stream
 * @author skgchxngsxyz-osx
 *
 */
@SharedClass
public class OutputStream {
	private final String streamName;
	private final BufferedWriter writer;
	private final boolean closeable;

	private OutputStream(java.io.OutputStream sourceOutput, String name, boolean closeable) {
		this.streamName = name;
		this.closeable = closeable;
		this.writer = new BufferedWriter(new OutputStreamWriter(sourceOutput));
	}

	public static OutputStream createOutputStream(java.io.OutputStream sourceOutput, String name, boolean closeable) {
		return new OutputStream(sourceOutput, name, closeable);
	}

	public static OutputStream createStdout() {
		return createOutputStream(System.out, "standard output", false);
	}

	public static OutputStream createStderr() {
		return createOutputStream(System.err, "standard error", false);
	}

	@Shared
	public void close() {
		if(this.closeable) {
			try {
				this.writer.close();
			} catch(IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	@Shared
	@Override
	public String toString() {
		return this.streamName;
	}

	@Shared
	public void writeLine(String line) {
		try {
			this.writer.write(line);
			this.writer.write("\n");
			this.writer.flush();
		} catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}
}
