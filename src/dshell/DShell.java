package dshell;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

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
		DShellConsole Console = new DShellConsole();
		boolean interactiveMode = true;
		ZenGenerator Generator = new ModifiedJavaScriptSourceGenerator(); //TODO: using JavaByteCodeGen
		LibNative.ImportGrammar(Generator.RootNameSpace, ZenGrammar.class.getName());
		LibNative.ImportGrammar(Generator.RootNameSpace, DShellGrammar.class.getName());
		if(interactiveMode) {
			System.out.println(ZenParserConst.ProgName + ZenParserConst.Version + " (" + ZenParserConst.CodeName + ") on " + LibZen.GetPlatform());
			System.out.println(ZenParserConst.Copyright);	//FIXME
			Generator.Logger.ShowReportedErrors();
			int linenum = 1;
			String Line = null;
			while ((Line = Console.ReadLine(">>> ", "    ")) != null) {
				try {
					Object EvaledValue = Generator.RootNameSpace.Eval(Line, linenum, interactiveMode);
					Generator.Logger.ShowReportedErrors();
					if (EvaledValue != null) {
						System.out.println(" (" + ZenSystem.GuessType(EvaledValue) + ":");
						System.out.println(LibNative.GetClassName(EvaledValue)+ ") ");
						System.out.println(LibZen.Stringify(EvaledValue));
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
	private jline.ConsoleReader ConsoleReader = null;
	
	public DShellConsole() {
		try {
			this.ConsoleReader = new jline.ConsoleReader();
			jline.SimpleCompletor CommandCompletor = new jline.SimpleCompletor("dummy");
			CommandCompletor.setCandidates(GetCommandListFromPath());
			jline.Completor[] Completors = new jline.Completor[2];
			Completors[0] = CommandCompletor;
			Completors[1] = new jline.FileNameCompletor();
			this.ConsoleReader.addCompletor(new jline.ArgumentCompletor(Completors));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final String ReadLine(String Prompt, String Prompt2) {
		String Line;
		try {
			Line = this.ConsoleReader.readLine(Prompt);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(Line == null) {
			System.exit(0);
		}
		if(Prompt2 != null) {
			int level = 0;
			while((level = LibZen.CheckBraceLevel(Line)) > 0) {
				String Line2;
				try {
					Line2 = this.ConsoleReader.readLine(Prompt2);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
				Line += "\n" + Line2;
			}
			if(level < 0) {
				Line = "";
				LibNative.println(" .. canceled");
			}
		}
		this.ConsoleReader.getHistory().addToHistory(Line);
		return Line;
	}

	private final TreeSet<String> GetCommandListFromPath() {
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
}
