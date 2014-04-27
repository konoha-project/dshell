package dshell.grammar;

import libbun.util.BMatchFunction;
import libbun.lang.konoha.ContinuePatternFunction;
import libbun.lang.bun.BunGrammar;
import libbun.parser.LibBunGamma;
import libbun.parser.LibBunSyntax;
import dshell.DShell;
import dshell.grammar.ShellGrammar;

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
		MatchRegexPatternFunc matchRegxPattern = new MatchRegexPatternFunc();
		SubstitutionPatternFunc substitutionPattern = new SubstitutionPatternFunc();
		DShellVarPatternFunc varPattern = new DShellVarPatternFunc();

		Gamma.DefineToken("\"", new DShellStringLiteralTokenFunc());

//		overrideStatement(Gamma, "import", new JavaImportPattern());
		overrideStatement(Gamma, "continue", new ContinuePatternFunction());

		Gamma.DefineBinaryOperator("=~", matchRegxPattern);
		Gamma.DefineBinaryOperator("!~", matchRegxPattern);

		overrideStatement(Gamma, "try", new DShellTryPatternFunc());
		overrideStatement(Gamma, DShellTryPatternFunc.CatchPatternName, new DShellCatchPatternFunc());
		Gamma.DefineStatement(location, new LocationDefinePatternFunc());
		Gamma.DefineStatement("for", new ForPatternFunc());
		Gamma.DefineStatement("for", new ForeachPatternFunc());
		Gamma.DefineExpression(DShellStringLiteralPatternFunc.PatternName, new DShellStringLiteralPatternFunc());
//		NameSpace.DefineExpression("$( `", substitutionPattern);
		Gamma.DefineExpression("$", substitutionPattern);
		Gamma.DefineExpression(SubstitutionPatternFunc._PatternName, substitutionPattern);
		overrideExpression(Gamma, "assert", new AssertPatternFunc());
		overrideExpression(Gamma, DShellBlockPatternFunc.PatternName, new DShellBlockPatternFunc());
		overrideStatement(Gamma, "var", varPattern);
		overrideStatement(Gamma, "let", varPattern);

		Gamma.Generator.LangInfo.AppendGrammarInfo("dshell" + DShell.version);
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
