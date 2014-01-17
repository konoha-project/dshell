package dshell;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import jnr.ffi.Struct.int16_t;

import dshell.codegen.javascript.ModifiedJavaScriptSourceGenerator;
import dshell.lang.DShellGrammar;
import zen.deps.LibNative;
import zen.deps.LibZen;
import zen.lang.ZenGrammar;
import zen.lang.ZenSystem;
import zen.parser.ZenGenerator;
import zen.parser.ZenParserConst;

public class DShell {
	public static void main(String[] args) {
		DShellConsole console = new DShellConsole();
		boolean interactiveMode = true;
		ZenGenerator generator = new ModifiedJavaScriptSourceGenerator(); //TODO: using JavaByteCodeGen
		LibNative.ImportGrammar(generator.RootNameSpace, ZenGrammar.class.getName());
		LibNative.ImportGrammar(generator.RootNameSpace, DShellGrammar.class.getName());
		if(interactiveMode) {
			System.out.println(ZenParserConst.ProgName + ZenParserConst.Version + " (" + ZenParserConst.CodeName + ") on " + LibZen.GetPlatform());
			System.out.println(ZenParserConst.Copyright);	//FIXME
			generator.Logger.ShowReportedErrors();
			int linenum = 1;
			String line = null;
			while ((line = console.readLine()) != null) {
				try {
					Object evaledValue = generator.RootNameSpace.Eval(line, linenum, interactiveMode);
					generator.Logger.ShowReportedErrors();
					if (evaledValue != null) {
						System.out.println(" (" + ZenSystem.GuessType(evaledValue) + ":");
						System.out.println(LibNative.GetClassName(evaledValue)+ ") ");
						System.out.println(LibZen.Stringify(evaledValue));
					}
					linenum++;
				}
				catch (Exception e) {
					LibZen.PrintStackTrace(e, linenum);
					linenum++;
				}
			}
			System.out.println("");
		}
	}
}

class DShellConsole {
	private jline.ConsoleReader consoleReader = null;
	private String userName = System.getProperty("user.name");
	
	public DShellConsole() {
		try {
			this.consoleReader = new jline.ConsoleReader();
			jline.SimpleCompletor commandCompletor = new jline.SimpleCompletor("dummy");
			commandCompletor.setCandidates(getCommandListFromPath());
			jline.Completor[] completors = new jline.Completor[2];
			completors[0] = commandCompletor;
			completors[1] = new jline.FileNameCompletor();
			this.consoleReader.addCompletor(new jline.ArgumentCompletor(completors));
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

	private final TreeSet<String> getCommandListFromPath() {
		TreeSet<String> commandSet = new TreeSet<String>();
		String[] paths = System.getenv("PATH").split(":");
		for(int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if(path.startsWith("~")) {
				path = System.getenv("HOME") + path.substring(1);
			}
			File file = new File(path);
			File[] files = file.listFiles();
			for(int j = 0; j < files.length; j++) {
				commandSet.add(files[j].getName());
			}
		}
		return commandSet;
	}

	private String[] getPrompts() {
		String homeDir = System.getenv("HOME");
		String[] prompts = new String[2];
		String currentDir = System.getProperty("user.dir");
		if(currentDir.startsWith(homeDir)) {
			int index = homeDir.length();
			currentDir = "~" + currentDir.substring(index);
		}
		String prompt =  "[" + this.userName + "]:" + currentDir + "> ";
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
