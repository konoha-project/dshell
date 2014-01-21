package dshell;

import dshell.codegen.javascript.ModifiedJavaScriptSourceGenerator;
import dshell.lang.DShellGrammar;
import dshell.util.DShellConsole;
import zen.deps.LibNative;
import zen.deps.LibZen;
import zen.deps.ZenArray;
import zen.lang.ZenGrammar;
import zen.lang.ZenSystem;
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
		ZGenerator generator = new ModifiedJavaScriptSourceGenerator(); //TODO: using JavaByteCodeGen
		LibNative.ImportGrammar(generator.RootNameSpace, ZenGrammar.class.getName());
		LibNative.ImportGrammar(generator.RootNameSpace, DShellGrammar.class.getName());
		if(interactiveMode) {
			DShellConsole console = new DShellConsole();
			showVersionInfo();
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
		else {
			String scriptName = args[0];
			ZenArray<String> ARGV = ZenArray.NewZenArray(ZenSystem.StringType);
			for(int i = 1; i < args.length; i++) {
				ARGV.add(args[i]);
			}
			generator.RootNameSpace.SetSymbol("ARGV", ARGV, null);
			String sourceText = LibNative.LoadScript(scriptName);
			if (sourceText == null) {
				LibNative.Exit(1, "file not found: " + scriptName);
			}
			long fileLine = ZenSystem.GetFileLine(scriptName, 1);
			boolean status = generator.RootNameSpace.Load(sourceText, fileLine);
			generator.Logger.ShowReportedErrors();
			if(!status) {
				LibNative.Exit(1, "abort loading: " + scriptName);
			}
		}
	}
}
