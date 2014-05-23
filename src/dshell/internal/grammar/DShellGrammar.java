package dshell.internal.grammar;

import libbun.util.BMatchFunction;
import libbun.lang.konoha.ContinuePatternFunction;
import libbun.lang.bun.BunGrammar;
import libbun.lang.bun.extra.BunExtraGrammar;
import libbun.parser.classic.LibBunGamma;
import libbun.parser.classic.LibBunSyntax;
import dshell.internal.grammar.ShellGrammar;
import dshell.internal.main.DShell;

public class DShellGrammar {	//FIXME
	public final static String location = "location";

	public static String toLocationSymbol(String symbol) {
		return "__@$" + symbol;
	}

	public static void ImportGrammar(LibBunGamma gamma) {
		// import BunGrammer
		BunGrammar.LoadGrammar(gamma);
		// import ShellGrammar
		ShellGrammar.LoadGrammar(gamma);
		// import DShell Specific Grammar
		MatchRegexPatternFunc matchRegxPattern = new MatchRegexPatternFunc();
		SubstitutionPatternFunc substitutionPattern = new SubstitutionPatternFunc();
		DShellVarPatternFunc varPattern = new DShellVarPatternFunc();

		gamma.DefineToken("'", new SingleQuoteStringLiteralTokenFunc());
		gamma.DefineToken("\"", new DoubleQuoteStringLiteralTokenFunc());

//		overrideStatement(Gamma, "import", new JavaImportPattern());
		overrideStatement(gamma, "continue", new ContinuePatternFunction());

		gamma.DefineBinaryOperator("=~", matchRegxPattern);
		gamma.DefineBinaryOperator("!~", matchRegxPattern);

		overrideStatement(gamma, "try", new DShellTryPatternFunc());
		overrideStatement(gamma, DShellTryPatternFunc.catchPatternName, new DShellCatchPatternFunc());
//		gamma.DefineStatement(location, new LocationDefinePatternFunc());
		gamma.DefineStatement("for", new ForPatternFunc());
		gamma.DefineStatement("for", new ForeachPatternFunc());
		gamma.DefineExpression(DoubleQuoteStringLiteralPatternFunc.patternName, new DoubleQuoteStringLiteralPatternFunc());
//		gamma.DefineExpression("$( `", substitutionPattern);
		gamma.DefineExpression("$", substitutionPattern);
		gamma.DefineExpression(SubstitutionPatternFunc.patternName, substitutionPattern);
		overrideExpression(gamma, "assert", new AssertPatternFunc());
		overrideExpression(gamma, DShellBlockPatternFunc.patternName, new DShellBlockPatternFunc());
		overrideStatement(gamma, "var", varPattern);
		overrideStatement(gamma, "let", varPattern);

		gamma.DefineExpressionSuffix("++", BunExtraGrammar.IncSuffixPattern);
		gamma.DefineExpressionSuffix("--", BunExtraGrammar.DecSuffixPattern);

		gamma.DefineBinaryOperator("+", BunExtraGrammar.SelfAddPattern);
		gamma.DefineBinaryOperator("-", BunExtraGrammar.SelfSubPattern);
		gamma.DefineBinaryOperator("*", BunExtraGrammar.SelfMulPattern);
		gamma.DefineBinaryOperator("/", BunExtraGrammar.SelfDivPattern);
		gamma.DefineBinaryOperator("%", BunExtraGrammar.SelfModPattern);
		gamma.DefineBinaryOperator("&", BunExtraGrammar.SelfBitwiseAndPattern);
		gamma.DefineBinaryOperator("|", BunExtraGrammar.SelfBitwiseOrPattern);
		gamma.DefineBinaryOperator("^", BunExtraGrammar.SelfBitwiseXorPattern);
		gamma.DefineBinaryOperator("<<", BunExtraGrammar.SelfLeftShiftPattern);
		gamma.DefineBinaryOperator(">>", BunExtraGrammar.SelfRightShiftPattern);

		gamma.Generator.LangInfo.AppendGrammarInfo("dshell" + DShell.version);
	}

	private static void overrideStatement(LibBunGamma gamma, String patternName, BMatchFunction matchFunc) {
		LibBunSyntax oldSyntaxPattern = gamma.GetSyntaxPattern(patternName);	//FIXME
		if(oldSyntaxPattern == null) {
			gamma.DefineStatement(patternName, matchFunc);
		}
		else {
			oldSyntaxPattern.MatchFunc = matchFunc;
			oldSyntaxPattern.ParentPattern = null;
			oldSyntaxPattern.SyntaxFlag = LibBunSyntax._Statement;
		}
	}

	private static void overrideExpression(LibBunGamma gamma, String patternName, BMatchFunction matchFunc) {
		LibBunSyntax oldSyntaxPattern = gamma.GetSyntaxPattern(patternName);	//FIXME
		if(oldSyntaxPattern == null) {
			gamma.DefineExpression(patternName, matchFunc);
		}
		else {
			oldSyntaxPattern.MatchFunc = matchFunc;
			oldSyntaxPattern.ParentPattern = null;
			oldSyntaxPattern.SyntaxFlag = 0;
		}
	}
}
