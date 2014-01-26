package dshell;

import zen.codegen.javascript.ModifiedJavaScriptSourceGenerator;
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

	public static void showVersionInfo() {
		System.out.println(progName + ", version " + version + " (" + LibZen.GetPlatform() + ")");
		System.out.println(copyright);
	}

	public static void main(String[] args) {
		boolean interactiveMode = true;
		if(args.length > 0) {
			interactiveMode = false;
		}

		ZenEngine engine = loadDShellEngine();
		if(interactiveMode) {
			DShellConsole console = new DShellConsole();
			showVersionInfo();
			engine.Generator.Logger.ShowReportedErrors();
			int linenum = 1;
			String line = null;
			while ((line = console.readLine()) != null) {
				try {
					Object evaledValue = engine.Eval(line, linenum, interactiveMode);
					engine.Generator.Logger.ShowReportedErrors();
					if (evaledValue != null) {
						System.out.println(" (" + ZSystem.GuessType(evaledValue) + ":");
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
		else {
			String scriptName = args[0];
			ZenArray<String> ARGV = ZenArray.NewZenArray(ZSystem.StringType);
			for(int i = 1; i < args.length; i++) {
				ARGV.add(args[i]);
			}
			engine.Generator.RootNameSpace.SetSymbol("ARGV", ARGV, null);
			String sourceText = LibNative.LoadTextFile(scriptName);
			if (sourceText == null) {
				LibNative.Exit(1, "file not found: " + scriptName);
			}
			long fileLine = ZSystem.GetFileLine(scriptName, 1);
			boolean status = engine.Load(sourceText, fileLine);
			engine.Generator.Logger.ShowReportedErrors();
			if(!status) {
				LibNative.Exit(1, "abort loading: " + scriptName);
			}
		}
	}

	private final static ZenEngine loadDShellEngine() {
		//ZGenerator generator = new ModifiedJavaScriptSourceGenerator(); //TODO: using JavaByteCodeGen
		ZGenerator generator = new ModifiedJavaByteCodeGenerator();
		LibNative.ImportGrammar(generator.RootNameSpace, DShellGrammar.class.getName());
		return generator.GetEngine();
	}
}
