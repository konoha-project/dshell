package dshell.util;

import java.io.File;
import java.io.IOException;

import dshell.lib.BuiltinCommandMap;

import zen.deps.LibZen;

public class DShellConsole {
	private jline.ConsoleReader consoleReader = null;
	private String userName = System.getProperty("user.name");
	
	public DShellConsole() {
		try {
			this.consoleReader = new jline.ConsoleReader();
			this.consoleReader.addCompletor(new DShellCompletor());
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
			while((level = LibZen.CheckBraceLevel(line)) > 0) {
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
		return line;
	}

	private String[] getPrompts() {
		String homeDir = System.getenv("HOME");
		String[] prompts = new String[2];
		//String currentDir = System.getProperty("user.dir");
		//String currentDir = new File(".").getAbsoluteFile().getParent();
		String currentDir = BuiltinCommandMap.getWorkingDirectory();
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
