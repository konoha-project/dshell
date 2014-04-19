package libbun.lang.bun.shell;

import java.util.ArrayList;

import libbun.ast.decl.BunLetVarNode;
import libbun.ast.literal.BunStringNode;
import libbun.encode.jvm.JavaImportPattern;
import libbun.type.BType;
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
		BunGrammar.ImportGrammar(Gamma);
		// import ShellGrammar
		ShellGrammar.ImportGrammar(Gamma);
		// import DShell Specific Grammar
		ImportCommandPatternFunction importCommandPattern = new DShellImportCommandPatternFunc();
//		ComparatorPatternFunction comparatorPattern = new ComparatorPatternFunction();
		SubstitutionPatternFunc substitutionPattern = new SubstitutionPatternFunc();
		DShellVarPatternFunc varPattern = new DShellVarPatternFunc();

		Gamma.AppendTokenFunc("\"", new DShellStringLiteralTokenFunc());

		overrideSyntaxPattern(Gamma, "import", new JavaImportPattern(), true);
		overrideSyntaxPattern(Gamma, "continue", new ContinuePatternFunction(), true);
		Gamma.DefineStatement("import", new DShellImportPatternFunc());
		Gamma.DefineExpression(ImportEnvPatternFunc.PatternName, new ImportEnvPatternFunc());
		Gamma.DefineStatement("command", importCommandPattern);
		overrideSyntaxPattern(Gamma, ImportCommandPatternFunction._PatternName, importCommandPattern, false);
		overrideSyntaxPattern(Gamma, SimpleArgumentPatternFunction._PatternName, new CommandArgPatternFunc(), false);

//		NameSpace.DefineRightExpression("=~", BunPrecedence._CStyleEquals, comparatorPattern);
//		NameSpace.DefineRightExpression("!~", BunPrecedence._CStyleEquals, comparatorPattern);
		overrideSyntaxPattern(Gamma, "try", new DShellTryPatternFunc(), true);
		overrideSyntaxPattern(Gamma, DShellTryPatternFunc.CatchPatternName, new DShellCatchPatternFunc(), true);
		Gamma.DefineStatement(location, new LocationDefinePatternFunc());
		Gamma.DefineStatement("for", new ForPatternFunc());
		Gamma.DefineStatement("for", new ForeachPatternFunc());
		Gamma.DefineStatement(ExportEnvPatternFunc.PatternName, new ExportEnvPatternFunc());
		Gamma.DefineExpression(DShellStringLiteralPatternFunc.PatternName, new DShellStringLiteralPatternFunc());
//		NameSpace.DefineExpression("$( `", substitutionPattern);
		Gamma.DefineExpression("$", substitutionPattern);
		Gamma.DefineExpression(SubstitutionPatternFunc._PatternName, substitutionPattern);
		overrideSyntaxPattern(Gamma, "assert", new AssertPatternFunc(), false);
		overrideSyntaxPattern(Gamma, DShellBlockPatternFunc.PatternName, new DShellBlockPatternFunc(), false);
		overrideSyntaxPattern(Gamma, "var", varPattern, true);
		overrideSyntaxPattern(Gamma, "let", varPattern, true);

		// from BultinCommandMap
		ArrayList<String> symbolList = BuiltinCommand.getCommandSymbolList();
		for(String symbol : symbolList) {
			setOptionalSymbol(Gamma, symbol);
		}
		Gamma.Generator.LangInfo.AppendGrammarInfo("dshell" + DShell.version);
	}

	private static void setOptionalSymbol(LibBunGamma Gamma, String symbol) { // FIXME: null
		BunLetVarNode LetNode = new BunLetVarNode(null, BunLetVarNode._IsReadOnly, BType.StringType, symbol);
		LetNode.SetNode(BunLetVarNode._InitValue, new BunStringNode(null, null, symbol));
		Gamma.SetSymbol(ShellUtils._ToCommandSymbol(symbol), LetNode);
	}

	private static void overrideSyntaxPattern(LibBunGamma Gamma, String PatternName, BMatchFunction MatchFunc, boolean isStatement) {
		LibBunSyntax Pattern = new LibBunSyntax(Gamma, PatternName, MatchFunc);
		Pattern.IsStatement = isStatement;
		Gamma.SetSyntaxPattern(PatternName, Pattern);
	}
}
