package dshell.lang;

import java.util.ArrayList;

import libbun.encode.jvm.JavaImportPattern;

import libbun.type.BType;
import libbun.util.BMatchFunction;
import libbun.lang.bun.ComparatorPatternFunction;
import libbun.ast.BBlockNode;
import libbun.ast.decl.BLetVarNode;
import libbun.ast.literal.BStringNode;
import libbun.lang.konoha.ContinuePatternFunction;
import libbun.lang.bun.BunPrecedence;
import libbun.lang.bun.BunGrammar;
import libbun.lang.bun.shell.ImportCommandPatternFunction;
import libbun.lang.bun.shell.ShellGrammar;
import libbun.lang.bun.shell.ShellUtils;
import libbun.lang.bun.shell.SimpleArgumentPatternFunction;
import libbun.parser.BNameSpace;
import libbun.parser.BSyntax;
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

	public static void ImportGrammar(BNameSpace NameSpace) {
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

	private static void setOptionalSymbol(BNameSpace NameSpace, String symbol) { // FIXME: null
		BLetVarNode LetNode = new BLetVarNode(null, BLetVarNode._IsReadOnly, BType.StringType, symbol);
		LetNode.SetNode(BLetVarNode._InitValue, new BStringNode(null, null, symbol));
		NameSpace.SetSymbol(ShellUtils._ToCommandSymbol(symbol), LetNode);
	}

	private static void overrideSyntaxPattern(BNameSpace NameSpace, String PatternName, BMatchFunction MatchFunc, boolean isStatement) {
		BSyntax Pattern = new BSyntax(NameSpace, PatternName, MatchFunc);
		Pattern.IsStatement = isStatement;
		NameSpace.SetSyntaxPattern(PatternName, Pattern);
	}
}
