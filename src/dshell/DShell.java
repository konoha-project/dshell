package dshell;

import dshell.codegen.ModifiedJavaScriptSourceGenerator;
import zen.deps.LibNative;
import zen.deps.LibZen;
import zen.lang.ZenGrammar;
import zen.lang.ZenSystem;
import zen.main.ZenMain;
import zen.parser.ZenGenerator;
import zen.parser.ZenParserConst;

public class DShell {
	public static void main(String[] args) {
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
			while ((Line = ZenMain.ReadLine2(">>> ", "    ")) != null) {
				try {
					Object EvaledValue = Generator.RootNameSpace.Eval(Line, linenum);
					Generator.Logger.ShowReportedErrors();
					if (EvaledValue != null) {
						System.out.println(" (" + ZenSystem.GuessType(EvaledValue) + ":");
						System.out.println(LibNative.GetClassName(EvaledValue)+ ") ");
						System.out.println(LibZen.Stringify(EvaledValue));
					}
					linenum++;
				} catch (Exception e) {
					LibZen.PrintStackTrace(e, linenum);
					linenum++;
				}
			}
			System.out.println("");
		}
	}
}
