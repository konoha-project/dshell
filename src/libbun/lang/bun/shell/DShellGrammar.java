package libbun.lang.bun.shell;

import java.util.ArrayList;

import libbun.encode.jvm.JavaImportPattern;
import libbun.util.BMatchFunction;
import libbun.lang.konoha.ContinuePatternFunction;
import libbun.lang.bun.BunPrecedence;
import libbun.lang.bun.BunGrammar;
import libbun.lang.bun.shell.ImportCommandPatternFunction;
import libbun.lang.bun.shell.ShellGrammar;
import libbun.lang.bun.shell.ShellUtils;
import libbun.lang.bun.shell.SimpleArgumentPatternFunction;
import libbun.parser.LibBunGamma;
import libbun.parser.LibBunSyntax;
import dshell.DShell;
import dshell.lib.BuiltinCommand;

public class DShellGrammar {	//FIXME
	public final static String location = "location";

	public static String toLocationSymbol(String Symbol) {
		return "__@$" + Symbol;
	}

	public static void ImportGrammar(LibBunGamma Gamma) {
		// import BunGrammer
		BunGrammar.LoadGrammar(Gamma);
		// import ShellGrammar
		ShellGrammar.LoadGrammar(Gamma);
		// import DShell Specific Grammar
		ImportCommandPatternFunction importCommandPattern = new DShellImportCommandPatternFunc();
		MatchRegxPatternFunc matchRegxPattern = new MatchRegxPatternFunc();
		SubstitutionPatternFunc substitutionPattern = new SubstitutionPatternFunc();
		DShellVarPatternFunc varPattern = new DShellVarPatternFunc();

		//Gamma.DefineToken("\"", new DShellStringLiteralTokenFunc());

		overrideStatement(Gamma, "import", new JavaImportPattern());
		overrideStatement(Gamma, "continue", new ContinuePatternFunction());
		Gamma.DefineStatement("import", new DShellImportPatternFunc());
		Gamma.DefineExpression(ImportEnvPatternFunc.PatternName, new ImportEnvPatternFunc());
		Gamma.DefineStatement("command", importCommandPattern);
		overrideExpression(Gamma, ImportCommandPatternFunction._PatternName, importCommandPattern);
		overrideExpression(Gamma, SimpleArgumentPatternFunction._PatternName, new CommandArgPatternFunc());

		Gamma.DefineBinaryOperator("=~", matchRegxPattern);
		Gamma.DefineBinaryOperator("!~", matchRegxPattern);

		overrideStatement(Gamma, "try", new DShellTryPatternFunc());
		overrideStatement(Gamma, DShellTryPatternFunc.CatchPatternName, new DShellCatchPatternFunc());
		Gamma.DefineStatement(location, new LocationDefinePatternFunc());
		Gamma.DefineStatement("for", new ForPatternFunc());
		Gamma.DefineStatement("for", new ForeachPatternFunc());
		Gamma.DefineStatement(ExportEnvPatternFunc.PatternName, new ExportEnvPatternFunc());
		Gamma.DefineExpression(DShellStringLiteralPatternFunc.PatternName, new DShellStringLiteralPatternFunc());
//		NameSpace.DefineExpression("$( `", substitutionPattern);
		Gamma.DefineExpression("$", substitutionPattern);
		Gamma.DefineExpression(SubstitutionPatternFunc._PatternName, substitutionPattern);
		overrideExpression(Gamma, "assert", new AssertPatternFunc());
		overrideExpression(Gamma, DShellBlockPatternFunc.PatternName, new DShellBlockPatternFunc());
		overrideStatement(Gamma, "var", varPattern);
		overrideStatement(Gamma, "let", varPattern);

		// from BultinCommandMap
		ArrayList<String> symbolList = BuiltinCommand.getCommandSymbolList();
		for(String symbol : symbolList) {
			setOptionalSymbol(Gamma, symbol);
		}
		Gamma.Generator.LangInfo.AppendGrammarInfo("dshell" + DShell.version);
	}

	private static void setOptionalSymbol(LibBunGamma Gamma, String symbol) {
		ShellUtils.SetCommand(symbol, symbol);
	}

	private static void overrideStatement(LibBunGamma Gamma, String PatternName, BMatchFunction MatchFunc) {
		LibBunSyntax oldSyntaxPattern = Gamma.GetSyntaxPattern(PatternName);	//FIXME
		if(oldSyntaxPattern == null) {
			Gamma.DefineStatement(PatternName, MatchFunc);
		}
		else {
			oldSyntaxPattern.MatchFunc = MatchFunc;
			oldSyntaxPattern.ParentPattern = null;
			oldSyntaxPattern.SyntaxFlag = LibBunSyntax._Statement;
		}
	}

	private static void overrideExpression(LibBunGamma Gamma, String PatternName, BMatchFunction MatchFunc) {
		LibBunSyntax oldSyntaxPattern = Gamma.GetSyntaxPattern(PatternName);	//FIXME
		if(oldSyntaxPattern == null) {
			Gamma.DefineExpression(PatternName, MatchFunc);
		}
		else {
			oldSyntaxPattern.MatchFunc = MatchFunc;
			oldSyntaxPattern.ParentPattern = null;
			oldSyntaxPattern.SyntaxFlag = 0;
		}
	}
}
