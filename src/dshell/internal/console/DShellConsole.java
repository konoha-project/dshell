package dshell.internal.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;

import dshell.internal.lib.RuntimeContext;
import dshell.internal.lib.Utils;
import jline.Terminal;
import jline.ANSIBuffer.ANSICodes;
import jline.UnixTerminal;

public class DShellConsole implements AbstractConsole {
	private final jline.ConsoleReader consoleReader;
	private String userName = System.getProperty("user.name");
	private final TTYConfigurator ttyConfig;
	private int lineNumber;

	private final static String welcomeMessage = "Welcome to D-Shell <https://github.com/konoha-project/dshell>";

	public DShellConsole() {
		try {
			this.consoleReader = new jline.ConsoleReader();
			this.consoleReader.addCompletor(new DShellCompletor());
			this.lineNumber = 1;
			// save jline tty config
			this.ttyConfig = TTYConfigurator.initConfigurator(this.consoleReader.getTerminal());
			System.out.println(welcomeMessage);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final int getLineNumber() {
		return this.lineNumber;
	}

	public void incrementLineNum(String line) {
		int size = line.length();
		int count = 1;
		for(int i = 0; i < size; i++) {
			char ch = line.charAt(i);
			if(ch == '\n') {
				count++;
			}
		}
		this.lineNumber += count;
	}

	public final String readLine() {
		String[] prompts = this.getPrompts();
		String prompt = prompts[0];
		String prompt2 = prompts[1];
		StringBuilder lineBuilder = new StringBuilder();
		// load jLine ttyConfig
		this.ttyConfig.loadJlineConfig();
		String line = this.readLine(prompt);
		lineBuilder.append(line);
		int level = 0;
		while((level = this.checkBraceLevel(line, level)) > 0) {
			line = this.readLine(prompt2);
			lineBuilder.append("\n");
			lineBuilder.append(line);
		}
		if(level < 0) {
			if(line == null) {
				return null;
			}
			System.out.println(" .. canceled");
			return "";
		}
		// load original ttyConfig
		this.ttyConfig.loadOriginalConfig();
		line = lineBuilder.toString().trim();
		this.consoleReader.getHistory().addToHistory(line);
		return line;
	}

	private String readLine(String prompt) {
		try {
			return this.consoleReader.readLine(prompt);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String[] getPrompts() {
		String homeDir = Utils.getEnv("HOME");
		String[] prompts = new String[2];
		String currentDir = RuntimeContext.getContext().getWorkingDirectory();
		if(currentDir.startsWith(homeDir)) {
			int index = homeDir.length();
			currentDir = "~" + currentDir.substring(index);
		}
		String prompt = this.userName + ":" + currentDir + "> ";
		StringBuilder promptBuilder = new StringBuilder();
		int size = prompt.length();
		for(int i = 0; i < size; i++) {
			promptBuilder.append(" ");
		}
		prompts[0] = prompt;
		prompts[1] = promptBuilder.toString();
		return prompts;
	}

	private int checkBraceLevel(String text, int level) {
		if(text == null) {
			return -1;
		}
		boolean foundDoubleQuote = false;
		boolean foundSingleQuote = false;
		int size = text.length();
		for(int i = 0; i < size; i++) {
			char ch = text.charAt(i);
			if(!foundSingleQuote && !foundDoubleQuote) {
				if(ch == '{' || ch == '[' || ch == '(') {
					level++;
				}
				if(ch == '}' || ch == ']' || ch == ')') {
					level--;
				}
				if(ch == '\'') {
					foundSingleQuote = true;
				}
				if(ch == '"') {
					foundDoubleQuote = true;
				}
			}
			else {
				if(ch == '\\') {
					i++;
					continue;
				}
				if(ch == '\'') {
					foundSingleQuote = !foundSingleQuote;
				}
				if(ch == '"') {
					foundDoubleQuote = !foundDoubleQuote;
				}
			}
		}
		return level;
	}
}

class ShutdownOp extends Thread {
	@Override public void run() {
		System.out.print(ANSICodes.attrib(0));
	}
}

class TTYConfigurator {
	private final String originalTTYConfig;
	private final String jlineTTYConfig;

	private TTYConfigurator(String originalTTYConfig, String jlineTTYConfig) {
		this.originalTTYConfig = originalTTYConfig;
		this.jlineTTYConfig = jlineTTYConfig;
	}

	public void loadOriginalConfig() {
		loadTTYConfig(this.originalTTYConfig);
	}

	public void loadJlineConfig() {
		loadTTYConfig(this.jlineTTYConfig);
	}

	private static void loadTTYConfig(String ttyConfig) {
		execStty(ttyConfig);
	}

	private static String saveTTYConfig() {
		return execStty("-g");
	}

	private static String execStty(String arg) {
		ByteArrayOutputStream streamBuffer = new ByteArrayOutputStream();
		ProcessBuilder procBuilder = new ProcessBuilder("stty", arg);
		procBuilder.inheritIO();
		procBuilder.redirectOutput(Redirect.PIPE);
		try {
			Process proc = procBuilder.start();
			final InputStream input = proc.getInputStream();
			final int size = 512;
			int readSize = 0;
			byte[] buffer = new byte[size];
			while((readSize = input.read(buffer, 0, size)) > -1) {
				streamBuffer.write(buffer, 0, readSize);
			}
			proc.waitFor();
		}
		catch(IOException e) {
			e.printStackTrace();
			Utils.fatal(1, "IO problem");
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
		return Utils.removeNewLine(streamBuffer.toString());
	}

	public static TTYConfigurator initConfigurator(Terminal term) {
		if(term instanceof UnixTerminal && System.console() != null) {
			UnixTerminal unixTerm = (UnixTerminal)term;
			String originalTTYConfig = (String) Utils.getValue(unixTerm, "ttyConfig");
			String jlineTTYConfig = saveTTYConfig();
			Runtime.getRuntime().addShutdownHook(new ShutdownOp());
			return new TTYConfigurator(originalTTYConfig, jlineTTYConfig);
		}
		return new NullConfigurator();
	}

	public static class NullConfigurator extends TTYConfigurator {
		private NullConfigurator() {
			super(null, null);
		}
		@Override
		public void loadOriginalConfig() {	// do nothing
		}
		@Override
		public void loadJlineConfig() {	// do nothing
		}
	}
}
