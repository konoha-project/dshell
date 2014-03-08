package dshell.console;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import dshell.lib.RuntimeContext;
import dshell.lib.TaskBuilder;
import dshell.lib.TaskOption;
import dshell.lib.Utils;

import jline.Terminal;
import jline.ANSIBuffer.ANSICodes;
import jline.UnixTerminal;
import zen.main.ZenMain;
import static dshell.lib.TaskOption.Behavior.returnable;
import static dshell.lib.TaskOption.RetType.StringType;

public class DShellConsole {
	private jline.ConsoleReader consoleReader = null;
	private String userName = System.getProperty("user.name");
	private final TTYConfigurator ttyConfig;

	public final static String welcomeMessage = "oooooooooo.            .oooooo..o oooo                  oooo  oooo  \n" +
                                                "`888'   `Y8b          d8P'    `Y8 `888                  `888  `888  \n" +
                                                " 888      888         Y88bo.       888 .oo.    .ooooo.   888   888  \n" +
                                                " 888      888          `\"Y8888o.   888P\"Y88b  d88' `88b  888   888  \n" +
                                                " 888      888 8888888      `\"Y88b  888   888  888ooo888  888   888  \n" +
                                                " 888     d88'         oo     .d8P  888   888  888    .o  888   888  \n" +
                                                "o888bood8P'           8\"\"88888P'  o888o o888o `Y8bod8P' o888o o888o \n\n" +
                                                "Welcome to D-Shell <https://github.com/konoha-project/dshell>\n";
	public DShellConsole() {
		try {
			this.consoleReader = new jline.ConsoleReader();
			this.consoleReader.addCompletor(new DShellCompletor());
			// save jline tty config
			this.ttyConfig = TTYConfigurator.initConfigurator(this.consoleReader.getTerminal());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final String readLine() {
		String[] prompts = this.getPrompts();
		String prompt = prompts[0];
		String prompt2 = prompts[1];
		String line;
		try {
			this.ttyConfig.loadJlineConfig();
			line = this.consoleReader.readLine(prompt);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(line == null) {
			System.exit(0);
		}
		if(prompt2 != null) {
			int level = 0;
			while((level = ZenMain.CheckBraceLevel(line)) > 0) {
				String Line2;
				try {
					Line2 = this.consoleReader.readLine(prompt2);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
				line += "\n" + Line2;
			}
			if(level < 0) {
				line = "";
				System.out.println(" .. canceled");
			}
		}
		this.consoleReader.getHistory().addToHistory(line);
		this.ttyConfig.loadOriginalConfig();
		return line;
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
		System.out.print(ANSICodes.attrib(36));
		loadTTYConfig(this.jlineTTYConfig);
	}

	private static void loadTTYConfig(String ttyConfig) {
		TaskOption option = TaskOption.of(StringType, returnable);
		ArrayList<ArrayList<String>> cmdsList = new ArrayList<ArrayList<String>>();
		ArrayList<String> cmdList = new ArrayList<String>();
		cmdList.add("stty");
		cmdList.add(ttyConfig);
		cmdsList.add(cmdList);
		new TaskBuilder(cmdsList, option).invoke();
	}

	private static String saveTTYConfig() {
		TaskOption option = TaskOption.of(StringType, returnable);
		ArrayList<ArrayList<String>> cmdsList = new ArrayList<ArrayList<String>>();
		ArrayList<String> cmdList = new ArrayList<String>();
		cmdList.add("stty");
		cmdList.add("-g");
		cmdsList.add(cmdList);
		return ((String) new TaskBuilder(cmdsList, option).invoke()).trim();
	}

	public static TTYConfigurator initConfigurator(Terminal term) {
		if(term instanceof UnixTerminal && System.console() != null) {
			UnixTerminal unixTerm = (UnixTerminal)term;
			Field field;
			try {
				field = unixTerm.getClass().getDeclaredField("ttyConfig");
				field.setAccessible(true);
				String originalTTYConfig = (String) field.get(unixTerm);
				String jlineTTYConfig = saveTTYConfig();

				System.out.print(ANSICodes.attrib(36));
				Runtime.getRuntime().addShutdownHook(new ShutdownOp());
				return new TTYConfigurator(originalTTYConfig, jlineTTYConfig);
			}
			catch(Exception e) {
				e.printStackTrace();
				Utils.fatal(1, "field access failed: ttyConfig");
			}
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
