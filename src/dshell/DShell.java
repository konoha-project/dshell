package dshell;

import java.io.PrintStream;

import zen.codegen.jvm.ModifiedJavaByteCodeGenerator;
import dshell.lang.DShellGrammar;
import dshell.util.DShellConsole;
import zen.deps.LibNative;
import zen.deps.LibZen;
import zen.deps.ZenArray;
import zen.lang.ZSystem;
import zen.lang.ZenEngine;
import zen.parser.ZGenerator;

public class DShell {
	public final static String progName  = "D-Shell";
	public final static String codeName  = "Reference Implementation of D-Script";
	public final static int majorVersion = 0;
	public final static int minerVersion = 1;
	public final static int patchLevel   = 0;
	public final static String version = "0.1";
	public final static String copyright = "Copyright (c) 2013-2014, Konoha project authors";
	public final static String license = "BSD-Style Open Source";
	public final static String shellInfo = progName + ", version " + version + " (" + LibZen.GetPlatform() + ")";

	private boolean interactiveMode = true;
	private boolean debugMode = false;
	private String scriptName = null;
	private String sourceText = null;
	private ZenArray<String> ARGV;

	private DShell(String[] args) {
		boolean foundScriptFile = false;
		for(int i = 0; i < args.length; i++) {
			String optionSymbol = args[i];
			if(!foundScriptFile && optionSymbol.startsWith("--")) {
				if(optionSymbol.equals("--version")) {
					showVersionInfo();
					System.exit(0);
				}
				else if(optionSymbol.equals("--debug")) {
					this.debugMode = true;
				}
				else if(optionSymbol.equals("--help")) {
					showHelpAndExit(0, System.out);
				}
				else {
					System.err.println("dshell: " + optionSymbol + ": invalid option");
					showHelpAndExit(1, System.err);
				}
			}
			else if(!foundScriptFile) {
				this.scriptName = optionSymbol;
				this.sourceText = LibNative.LoadTextFile(this.scriptName);
				if (this.sourceText == null) {
					LibNative.Exit(1, "file not found: " + this.scriptName);
				}
				foundScriptFile = true;
				this.interactiveMode = false;
				this.ARGV = ZenArray.NewZenArray(ZSystem.StringType);
			}
			else {
				this.ARGV.add(optionSymbol);
			}
		}
	}

	private void execute() {
		ZenEngine engine = loadDShellEngine();
		if(this.interactiveMode) {
			DShellConsole console = new DShellConsole();
			showVersionInfo();
			engine.Generator.Logger.ShowReportedErrors();
			int linenum = 1;
			String line = null;
			while ((line = console.readLine()) != null) {
				if(line.trim().equals("")) {
					continue;
				}
				try {
					Object evaledValue = engine.Eval(line, linenum, interactiveMode);
					engine.Generator.Logger.ShowReportedErrors();
					if (this.debugMode && evaledValue != null) {
						System.out.println(" (" + ZSystem.GuessType(evaledValue) + ":");
						System.out.println(LibNative.GetClassName(evaledValue)+ ") ");
						System.out.println(LibZen.Stringify(evaledValue));
					}
				}
				catch (Exception e) {
					LibZen.PrintStackTrace(e, linenum);
				}
				linenum++;
			}
			System.out.println("");
		}
		else {
			engine.Generator.RootNameSpace.SetSymbol("ARGV", this.ARGV, null);
			long fileLine = ZSystem.GetFileLine(this.scriptName, 1);
			boolean status = engine.Load(this.sourceText, fileLine);
			engine.Generator.Logger.ShowReportedErrors();
			if(!status) {
				LibNative.Exit(1, "abort loading: " + this.scriptName);
			}
		}
	}

	public static void showVersionInfo() {
		System.out.println(shellInfo);
		System.out.println(copyright);
	}

	public static void showHelpAndExit(int status, PrintStream stream) {
		stream.println(shellInfo);
		stream.println("Usage: dshell [<options>] [<script-file> <argument> ...]");
		stream.println("Options:");
		stream.println("    --debug");
		stream.println("    --help");
		stream.println("    --version");
		System.exit(status);
	}

	private final static ZenEngine loadDShellEngine() {
		ZGenerator generator = new ModifiedJavaByteCodeGenerator();
		LibNative.ImportGrammar(generator.RootNameSpace, DShellGrammar.class.getName());
		return generator.GetEngine();
	}

	public static void main(String[] args) {
		new DShell(args).execute();
	}
}
