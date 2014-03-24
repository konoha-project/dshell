package dshell.lang;

import java.util.ArrayList;

import zen.util.ZMatchFunction;
import zen.grammar.ComparatorPatternFunction;
import zen.ast.ZBlockNode;
import zen.ast.ZStringNode;
import zen.codegen.jvm.JavaImportPattern;
import zen.lang.konoha.ContinuePatternFunction;
import zen.lang.zen.ZenPrecedence;
import zen.lang.zen.ZenGrammar;
import zen.parser.ZNameSpace;
import zen.parser.ZSyntax;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;
import dshell.DShell;
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
		// import ZenGrammer
		ZenGrammar.ImportGrammar(NameSpace);
		overrideSyntaxPattern(NameSpace, "import", new JavaImportPattern(), true);
		overrideSyntaxPattern(NameSpace, "continue", new ContinuePatternFunction(), true);

		// import DShell Specific Grammar
		CommandSymbolTokenFunc commandSymbolToken = new CommandSymbolTokenFunc();
		ImportCommandPatternFunc importCommandPattern = new ImportCommandPatternFunc();
		CommandSymbolPatternFunc commandSymbolPattern = new CommandSymbolPatternFunc();
		ComparatorPatternFunction comparatorPattern = new ComparatorPatternFunction();
		PrefixOptionPatternFunc prefixOptionPattern = new PrefixOptionPatternFunc();

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
		NameSpace.DefineRightExpression("=~", ZenPrecedence._CStyleEquals, comparatorPattern);
		NameSpace.DefineRightExpression("!~", ZenPrecedence._CStyleEquals, comparatorPattern);
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

		// from BultinCommandMap
		ArrayList<String> symbolList = BuiltinCommand.getCommandSymbolList();
		for(String symbol : symbolList) {
			setOptionalSymbol(NameSpace, symbol);
		}
		NameSpace.Generator.LangInfo.AppendGrammarInfo("dshell" + DShell.version);
	}

	private static void setOptionalSymbol(ZNameSpace NameSpace, String symbol) { // FIXME: null
		NameSpace.SetGlobalSymbol(DShellGrammar.toCommandSymbol(symbol), new ZStringNode(new ZBlockNode(null, NameSpace), null, symbol));
	}

	private static void overrideSyntaxPattern(ZNameSpace NameSpace, String PatternName, ZMatchFunction MatchFunc, boolean isStatement) {
		ZSyntax Pattern = new ZSyntax(NameSpace, PatternName, MatchFunc);
		Pattern.IsStatement = isStatement;
		NameSpace.SetSyntaxPattern(PatternName, Pattern);
	}
}
