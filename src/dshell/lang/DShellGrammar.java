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
import libbun.parser.ZNameSpace;
import libbun.parser.ZSyntax;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;
import dshell.DShell;
import dshell.grammar.AssertPatternFunc;
import dshell.grammar.CommandArgPatternFunc;
import dshell.grammar.CommandSymbolTokenFunc;
import dshell.grammar.ExportEnvPatternFunc;
import dshell.grammar.ForPatternFunc;
import dshell.grammar.ImportCommandPatternFunc;
import dshell.grammar.DShellCatchPatternFunc;
import dshell.grammar.DShellImportPatternFunc;
import dshell.grammar.CommandSymbolPatternFunc;
import dshell.grammar.DShellTryPatternFunc;
import dshell.grammar.ImportEnvPatternFunc;
import dshell.grammar.ForeachPatternFunc;
import dshell.grammar.InterStringLiteralPatternFunc;
import dshell.grammar.InterStringLiteralTokenFunc;
import dshell.grammar.LocationDefinePatternFunc;
import dshell.grammar.PrefixOptionPatternFunc;
import dshell.grammar.RedirectPatternFunc;
import dshell.grammar.ShellStyleCommentTokenFunc;
import dshell.grammar.SubstitutionPatternFunc;
import dshell.grammar.SuffixOptionPatternFunc;
import dshell.lib.BuiltinCommand;

public class DShellGrammar {
	// suffix option symbol
	public final static String background = "&";
	// prefix option symbol 
	public final static String timeout = "timeout";
	public final static String trace = "trace";
	public final static String location = "location";

	public static String toCommandSymbol(String Symbol) {
		return "__$" + Symbol;
	}

	public static String toLocationSymbol(String Symbol) {
		return "__@$" + Symbol;
	}

	public static boolean MatchStopToken(ZTokenContext TokenContext) { // ;,)]}&&||
		ZToken Token = TokenContext.GetToken();
		if(!TokenContext.HasNext()) {
			return true;
		}
		if(Token.IsIndent() || Token.EqualsText(";")) {
			return true;
		}
		if(Token.EqualsText(",") || Token.EqualsText(")") || Token.EqualsText("]") || 
				Token.EqualsText("}") || Token.EqualsText("&&") || Token.EqualsText("||") || Token.EqualsText("`")) {
			return true;
		}
		return false;
	}

	public static void ImportGrammar(ZNameSpace NameSpace) {
		// import BunGrammer
		BunGrammar.ImportGrammar(NameSpace);
		overrideSyntaxPattern(NameSpace, "import", new JavaImportPattern(), true);
		overrideSyntaxPattern(NameSpace, "continue", new ContinuePatternFunction(), true);

		// import DShell Specific Grammar
		CommandSymbolTokenFunc commandSymbolToken = new CommandSymbolTokenFunc();
		ImportCommandPatternFunc importCommandPattern = new ImportCommandPatternFunc();
		CommandSymbolPatternFunc commandSymbolPattern = new CommandSymbolPatternFunc();
		ComparatorPatternFunction comparatorPattern = new ComparatorPatternFunction();
		PrefixOptionPatternFunc prefixOptionPattern = new PrefixOptionPatternFunc();
		SubstitutionPatternFunc substitutionPattern = new SubstitutionPatternFunc();

		NameSpace.AppendTokenFunc("#", new ShellStyleCommentTokenFunc());
		NameSpace.AppendTokenFunc("Aa_", commandSymbolToken);
		NameSpace.AppendTokenFunc("1", commandSymbolToken);
		NameSpace.AppendTokenFunc("\"", new InterStringLiteralTokenFunc());

		NameSpace.DefineStatement("import", new DShellImportPatternFunc());
		NameSpace.DefineExpression(ImportEnvPatternFunc.PatternName, new ImportEnvPatternFunc());
		NameSpace.DefineStatement("command", importCommandPattern);
		NameSpace.DefineExpression(ImportCommandPatternFunc.PatternName, importCommandPattern);
		NameSpace.DefineExpression(CommandArgPatternFunc.PatternName, new CommandArgPatternFunc());
		NameSpace.DefineExpression(RedirectPatternFunc.PatternName, new RedirectPatternFunc());
		NameSpace.DefineExpression(SuffixOptionPatternFunc.PatternName, new SuffixOptionPatternFunc());
		NameSpace.DefineExpression(CommandSymbolPatternFunc.PatternName, commandSymbolPattern);
		NameSpace.DefineRightExpression("=~", BunPrecedence._CStyleEquals, comparatorPattern);
		NameSpace.DefineRightExpression("!~", BunPrecedence._CStyleEquals, comparatorPattern);
		overrideSyntaxPattern(NameSpace, "try", new DShellTryPatternFunc(), true);
		overrideSyntaxPattern(NameSpace, DShellTryPatternFunc.CatchPatternName, new DShellCatchPatternFunc(), true);
		NameSpace.DefineStatement(location, new LocationDefinePatternFunc());
		NameSpace.DefineExpression(timeout, prefixOptionPattern);
		NameSpace.DefineExpression(trace, prefixOptionPattern);
		NameSpace.DefineExpression(PrefixOptionPatternFunc.PatternName, prefixOptionPattern);
		NameSpace.DefineStatement("for", new ForPatternFunc());
		NameSpace.DefineStatement("for", new ForeachPatternFunc());
		NameSpace.DefineStatement(ExportEnvPatternFunc.PatternName, new ExportEnvPatternFunc());
		NameSpace.DefineExpression(InterStringLiteralPatternFunc.PatternName, new InterStringLiteralPatternFunc());
//		NameSpace.DefineExpression("$( `", substitutionPattern);
		NameSpace.DefineExpression("$", substitutionPattern);
		NameSpace.DefineExpression(SubstitutionPatternFunc.PatternName, substitutionPattern);
		overrideSyntaxPattern(NameSpace, "assert", new AssertPatternFunc(), false);

		// from BultinCommandMap
		ArrayList<String> symbolList = BuiltinCommand.getCommandSymbolList();
		for(String symbol : symbolList) {
			setOptionalSymbol(NameSpace, symbol);
		}
		NameSpace.Generator.LangInfo.AppendGrammarInfo("dshell" + DShell.version);
	}

	private static void setOptionalSymbol(ZNameSpace NameSpace, String symbol) { // FIXME: null
		NameSpace.SetSymbol(DShellGrammar.toCommandSymbol(symbol), new ZStringNode(new ZBlockNode(null, NameSpace), null, symbol));
	}

	private static void overrideSyntaxPattern(ZNameSpace NameSpace, String PatternName, ZMatchFunction MatchFunc, boolean isStatement) {
		ZSyntax Pattern = new ZSyntax(NameSpace, PatternName, MatchFunc);
		Pattern.IsStatement = isStatement;
		NameSpace.SetSyntaxPattern(PatternName, Pattern);
	}
}
