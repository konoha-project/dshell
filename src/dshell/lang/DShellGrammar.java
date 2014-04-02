package dshell.lang;

import java.util.ArrayList;

import libbun.encode.jvm.JavaImportPattern;

import libbun.util.ZMatchFunction;
import libbun.lang.bun.ComparatorPatternFunction;
import libbun.parser.ast.ZBlockNode;
import libbun.parser.ast.ZStringNode;
import libbun.lang.konoha.ContinuePatternFunction;
import libbun.lang.bun.BunPrecedence;
import libbun.lang.bun.BunGrammar;
import libbun.lang.bun.shell.ImportCommandPatternFunction;
import libbun.lang.bun.shell.ShellGrammar;
import libbun.lang.bun.shell.ShellUtils;
import libbun.lang.bun.shell.SimpleArgumentPatternFunction;
import libbun.parser.ZNameSpace;
import libbun.parser.ZSyntax;
import dshell.DShell;
import dshell.grammar.AssertPatternFunc;
import dshell.grammar.CommandArgPatternFunc;
import dshell.grammar.DShellImportCommandPatternFunc;
import dshell.grammar.ExportEnvPatternFunc;
import dshell.grammar.ForPatternFunc;
import dshell.grammar.DShellCatchPatternFunc;
import dshell.grammar.DShellImportPatternFunc;
import dshell.grammar.DShellTryPatternFunc;
import dshell.grammar.ImportEnvPatternFunc;
import dshell.grammar.ForeachPatternFunc;
import dshell.grammar.DShellStringLiteralPatternFunc;
import dshell.grammar.DShellStringLiteralTokenFunc;
import dshell.grammar.LocationDefinePatternFunc;
import dshell.grammar.SubstitutionPatternFunc;
import dshell.lib.BuiltinCommand;

public class DShellGrammar {
	public final static String location = "location";

	public static String toLocationSymbol(String Symbol) {
		return "__@$" + Symbol;
	}

	public static void ImportGrammar(ZNameSpace NameSpace) {
		// import BunGrammer
		BunGrammar.ImportGrammar(NameSpace);
		// import ShellGrammar
		ShellGrammar.ImportGrammar(NameSpace);
		// import DShell Specific Grammar
		ImportCommandPatternFunction importCommandPattern = new DShellImportCommandPatternFunc();
		ComparatorPatternFunction comparatorPattern = new ComparatorPatternFunction();
		SubstitutionPatternFunc substitutionPattern = new SubstitutionPatternFunc();

		NameSpace.AppendTokenFunc("\"", new DShellStringLiteralTokenFunc());

		overrideSyntaxPattern(NameSpace, "import", new JavaImportPattern(), true);
		overrideSyntaxPattern(NameSpace, "continue", new ContinuePatternFunction(), true);
		NameSpace.DefineStatement("import", new DShellImportPatternFunc());
		NameSpace.DefineExpression(ImportEnvPatternFunc.PatternName, new ImportEnvPatternFunc());
		NameSpace.DefineStatement("command", importCommandPattern);
		overrideSyntaxPattern(NameSpace, ImportCommandPatternFunction._PatternName, importCommandPattern, false);
		overrideSyntaxPattern(NameSpace, SimpleArgumentPatternFunction._PatternName, new CommandArgPatternFunc(), false);

		NameSpace.DefineRightExpression("=~", BunPrecedence._CStyleEquals, comparatorPattern);
		NameSpace.DefineRightExpression("!~", BunPrecedence._CStyleEquals, comparatorPattern);
		overrideSyntaxPattern(NameSpace, "try", new DShellTryPatternFunc(), true);
		overrideSyntaxPattern(NameSpace, DShellTryPatternFunc.CatchPatternName, new DShellCatchPatternFunc(), true);
		NameSpace.DefineStatement(location, new LocationDefinePatternFunc());
		NameSpace.DefineStatement("for", new ForPatternFunc());
		NameSpace.DefineStatement("for", new ForeachPatternFunc());
		NameSpace.DefineStatement(ExportEnvPatternFunc.PatternName, new ExportEnvPatternFunc());
		NameSpace.DefineExpression(DShellStringLiteralPatternFunc.PatternName, new DShellStringLiteralPatternFunc());
//		NameSpace.DefineExpression("$( `", substitutionPattern);
		NameSpace.DefineExpression("$", substitutionPattern);
		NameSpace.DefineExpression(SubstitutionPatternFunc._PatternName, substitutionPattern);
		overrideSyntaxPattern(NameSpace, "assert", new AssertPatternFunc(), false);

		// from BultinCommandMap
		ArrayList<String> symbolList = BuiltinCommand.getCommandSymbolList();
		for(String symbol : symbolList) {
			setOptionalSymbol(NameSpace, symbol);
		}
		NameSpace.Generator.LangInfo.AppendGrammarInfo("dshell" + DShell.version);
	}

	private static void setOptionalSymbol(ZNameSpace NameSpace, String symbol) { // FIXME: null
		NameSpace.SetSymbol(ShellUtils._ToCommandSymbol(symbol), new ZStringNode(new ZBlockNode(null, NameSpace), null, symbol));
	}

	private static void overrideSyntaxPattern(ZNameSpace NameSpace, String PatternName, ZMatchFunction MatchFunc, boolean isStatement) {
		ZSyntax Pattern = new ZSyntax(NameSpace, PatternName, MatchFunc);
		Pattern.IsStatement = isStatement;
		NameSpace.SetSyntaxPattern(PatternName, Pattern);
	}
}
