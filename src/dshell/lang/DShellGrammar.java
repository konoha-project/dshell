package dshell.lang;

import java.util.ArrayList;

import libbun.encode.jvm.JavaImportPattern;

import libbun.util.ZArray;
import libbun.util.ZMatchFunction;
import libbun.lang.bun.ComparatorPatternFunction;
import libbun.parser.ast.ZBinaryNode;
import libbun.parser.ast.ZBlockNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.lang.konoha.ContinuePatternFunction;
import libbun.lang.bun.BunPrecedence;
import libbun.lang.bun.BunGrammar;
import libbun.parser.ZNameSpace;
import libbun.parser.ZSource;
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
import dshell.grammar.DShellStringLiteralPatternFunc;
import dshell.grammar.DShellStringLiteralTokenFunc;
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

	public static ZNode ToNode(ZNode ParentNode, ZTokenContext TokenContext, ZArray<ZNode> NodeList) {
		ZToken Token = TokenContext.GetToken();
		ZNode Node = new ZStringNode(ParentNode, null, "");
		ZSyntax Pattern = TokenContext.NameSpace.GetRightSyntaxPattern("+");
		ZToken PlusToken = new ZToken(new ZSource(Token.GetFileName(), Token.GetLineNumber(), "+", TokenContext), 0, "+".length());
		for(ZNode CurrentNode : NodeList.ArrayValues) {
			ZBinaryNode BinaryNode = new ZBinaryNode(ParentNode, PlusToken, Node, Pattern);
			BinaryNode.SetNode(ZBinaryNode._Right, CurrentNode);
			Node = BinaryNode;
		}
		return Node;
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
		NameSpace.AppendTokenFunc("\"", new DShellStringLiteralTokenFunc());

		NameSpace.DefineStatement("import", new DShellImportPatternFunc());
		NameSpace.DefineExpression(ImportEnvPatternFunc.PatternName, new ImportEnvPatternFunc());
		NameSpace.DefineStatement("command", importCommandPattern);
		NameSpace.DefineExpression(ImportCommandPatternFunc._PatternName, importCommandPattern);
		NameSpace.DefineExpression(CommandArgPatternFunc._PatternName, new CommandArgPatternFunc());
		NameSpace.DefineExpression(RedirectPatternFunc._PatternName, new RedirectPatternFunc());
		NameSpace.DefineExpression(SuffixOptionPatternFunc._PatternName, new SuffixOptionPatternFunc());
		NameSpace.DefineExpression(CommandSymbolPatternFunc._PatternName, commandSymbolPattern);
		NameSpace.DefineRightExpression("=~", BunPrecedence._CStyleEquals, comparatorPattern);
		NameSpace.DefineRightExpression("!~", BunPrecedence._CStyleEquals, comparatorPattern);
		overrideSyntaxPattern(NameSpace, "try", new DShellTryPatternFunc(), true);
		overrideSyntaxPattern(NameSpace, DShellTryPatternFunc.CatchPatternName, new DShellCatchPatternFunc(), true);
		NameSpace.DefineStatement(location, new LocationDefinePatternFunc());
		NameSpace.DefineExpression(timeout, prefixOptionPattern);
		NameSpace.DefineExpression(trace, prefixOptionPattern);
		NameSpace.DefineExpression(PrefixOptionPatternFunc._PatternName, prefixOptionPattern);
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
		NameSpace.SetSymbol(DShellGrammar.toCommandSymbol(symbol), new ZStringNode(new ZBlockNode(null, NameSpace), null, symbol));
	}

	private static void overrideSyntaxPattern(ZNameSpace NameSpace, String PatternName, ZMatchFunction MatchFunc, boolean isStatement) {
		ZSyntax Pattern = new ZSyntax(NameSpace, PatternName, MatchFunc);
		Pattern.IsStatement = isStatement;
		NameSpace.SetSyntaxPattern(PatternName, Pattern);
	}
}
