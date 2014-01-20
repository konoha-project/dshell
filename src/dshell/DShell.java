package dshell;

import dshell.codegen.javascript.ModifiedJavaScriptSourceGenerator;
import dshell.lang.DShellGrammar;
import dshell.util.DShellConsole;
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
