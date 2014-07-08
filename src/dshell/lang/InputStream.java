package dshell.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import dshell.annotation.Shared;
import dshell.annotation.SharedClass;

/**
 * D-Shell input stream
 * @author skgchxngsxyz-osx
 *
 */
@SharedClass
public class InputStream {
	/**
	 * not null
	 */
	private final String streamName;
	private final BufferedReader reader;
	private final boolean closeable;

	private InputStream(java.io.InputStream sourceInput, String name, boolean closeable) {
		this.streamName = name;
		this.closeable = closeable;
		this.reader = new BufferedReader(new InputStreamReader(sourceInput));
	}

	public static InputStream createInputStream(java.io.InputStream sourceInput, String name, boolean closeable) {
		return new InputStream(sourceInput, name, closeable);
	}

	public static InputStream createStdin() {
		return createInputStream(System.in, "standard input", false);
	}

	@Shared
	public void close() {
		if(this.closeable) {
			try {
				this.reader.close();
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
	public String readLine() {
		String line = null;
		try {
			line = this.reader.readLine();
		} catch(IOException e) {
			System.err.println(e.getMessage());
		}
		return line == null ? "" : line;
	}
}
