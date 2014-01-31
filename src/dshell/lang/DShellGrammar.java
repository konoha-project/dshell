package dshell.lang;

import java.util.ArrayList;

import zen.ast.ZCatchNode;
import zen.ast.ZEmptyNode;
import zen.ast.ZErrorNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.deps.LibNative;
import zen.deps.LibZen;
import zen.deps.Var;
import zen.lang.ZFunc;
import zen.lang.ZenGrammar;
import zen.lang.ZenPrecedence;
import zen.parser.ZLogger;
import zen.parser.ZNameSpace;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;
import dshell.ast.DShellCommandNode;
import dshell.ast.DShellDummyNode;
import dshell.lib.BuiltinCommand;
import dshell.util.Utils;

public class DShellGrammar {
	// suffix option symbol
	public final static String background = "&";
	// prefix option symbol 
	public final static String timeout = "timeout";
	public final static String trace = "trace";

	private static String CommandSymbol(String Symbol) {
		return "__$" + Symbol;
	}

	private static void AppendCommand(ZNameSpace NameSpace, String CommandPath, ZToken KeyToken, ZToken SourceToken) {
		if(CommandPath.length() == 0) {
			return;
		}
		int loc = CommandPath.lastIndexOf('/');
		String Command = CommandPath;
		if(loc != -1) {
			if(!Utils.isFileExecutable(CommandPath)) {
				NameSpace.Generator.Logger.Report(ZLogger.ErrorLevel, SourceToken, "not executable: " + CommandPath);
				return;
			}
			Command = CommandPath.substring(loc + 1);
		}
		else {
			if(!Utils.isUnixCommand(CommandPath)) {
				NameSpace.Generator.Logger.Report(ZLogger.ErrorLevel, SourceToken, "unknown command: " + CommandPath);
				return;
			}
		}
		NameSpace.DefineSyntax(Command, MatchDShell);
		String CommandPrefix = KeyToken.ParsedText;
		if(!CommandPrefix.equals(SourceToken.ParsedText)) {	//FIXME: check duplication
			NameSpace.DefineSyntax(CommandPrefix, MatchDShell);
		}
		NameSpace.SetLocalSymbol(CommandSymbol(Command), new ZStringNode(SourceToken, CommandPath));
	}

	public static long ShellCommentToken(ZTokenContext TokenContext, String SourceText, long pos) {
		if(LibZen.CharAt(SourceText, pos) == '#') { // shell style SingleLineComment
			long NextPos = pos + 1;
			while(NextPos < SourceText.length()) {
				char NextChar = LibZen.CharAt(SourceText, NextPos);
				if(NextChar == '\n') {
					break;
				}
				NextPos++;
			}
			return ZenGrammar.IndentToken(TokenContext, SourceText, NextPos);
		}
		return ZTokenContext.MismatchedPosition;
	}

	public static ZNode MatchImport(ZNameSpace NameSpace, ZTokenContext TokenContext, ZNode LeftNode) {
		TokenContext.GetTokenAndMoveForward();
		if(TokenContext.IsToken("command")) {
			return TokenContext.ParsePattern(NameSpace, "$Command$", ZTokenContext.Required2);
		}
		if(TokenContext.IsToken("env")) {
			return TokenContext.ParsePattern(NameSpace, "$Env$", ZTokenContext.Required2);
		}
		return null;
	}

	public static ZNode MatchEnv(ZNameSpace NameSpace, ZTokenContext TokenContext, ZNode LeftNode) {
		TokenContext.GetTokenAndMoveForward();
		ZNode Node = TokenContext.ParsePattern(NameSpace, "$Identifier$", ZTokenContext.Required2);
		if(Node.IsErrorNode()) {
			return Node;
		}
		ZToken Token = Node.SourceToken;	// FIXME
//		String Name = Token.ParsedText;
//		String Env = System.getenv(Name);
//		Env = (Env == null) ? "" : Env;
//		NameSpace.SetSymbol(Name, Env, Token);
//		return new ZEmptyNode(Token);
		return new DShellDummyNode();
	}

	public static ZNode MatchCommand(ZNameSpace NameSpace, ZTokenContext TokenContext, ZNode LeftNode) {
		String Command = "";
		boolean foundSlash = true;
		ZToken KeyToken = null;
		long lineNum = TokenContext.GetToken().FileLine;
		ZToken SourceToken = null;
		String ParsedText = null;
		TokenContext.GetTokenAndMoveForward();
		while(TokenContext.HasNext()) {
			ZToken Token = TokenContext.GetTokenAndMoveForward();
			ParsedText = Token.ParsedText;
			if(foundSlash && !Token.EqualsText("/")) {
				foundSlash = false;
				KeyToken = Token;
			}
			if(Token.EqualsText(",")) {
				ParsedText = "";
			}
			if(Token.EqualsText("~")) {
				ParsedText = System.getenv("HOME");
			}
			if(Token.EqualsText("/")) {
				foundSlash = true;
			}
			if(Token.IsDelim() || Token.IsIndent()) {
				break;
			}
			Command += ParsedText;
			if(Token.IsNextWhiteSpace()) {
				SourceToken = new ZToken(0, ParsedText, lineNum);
				AppendCommand(NameSpace, Command, KeyToken, SourceToken);
				Command = "";
			}
		}
		if(!Command.equals("")) {
			SourceToken = new ZToken(0, Command, lineNum);
			AppendCommand(NameSpace, Command, KeyToken, SourceToken);
		}
		return new DShellDummyNode();
	}

	private static boolean MatchStopToken(ZTokenContext TokenContext) { // ;,)]}&&||
		ZToken Token = TokenContext.GetToken();
		if(!TokenContext.HasNext()) {
			return true;
		}
		if(Token.IsIndent() || Token.IsDelim()) {
			return true;
		}
		if(Token.EqualsText(",") || Token.EqualsText(")") || Token.EqualsText("]") || 
				Token.EqualsText("}") || Token.EqualsText("&&") || Token.EqualsText("||")) {
			return true;
		}
		return false;
	}

	public static ZNode MatchArgument(ZNameSpace NameSpace, ZTokenContext TokenContext, ZNode LeftNode) {
		if(MatchStopToken(TokenContext)) {
			return null;
		}
		ZToken Token = TokenContext.GetToken();
		boolean HasStringExpr = false;
		String Path = null;
		if(Token.IsQuoted()) {
			Path = LibZen.UnquoteString(Token.ParsedText);
			if(Path.indexOf("${") != -1) {
				HasStringExpr = true;
			}
			TokenContext.GetTokenAndMoveForward();
		}
		if(Path == null) {
			boolean FoundOpen = false;
			Path = "";
			while(TokenContext.HasNext()) {
				Token = TokenContext.GetToken();
				String ParsedText = Token.ParsedText;
				if(Token.IsIndent() || (!FoundOpen && MatchStopToken(TokenContext))) {
					break;
				}
				TokenContext.GetTokenAndMoveForward();
				if(Token.EqualsText("$")) {   // $HOME/hoge
					ZToken Token2 = TokenContext.GetToken();
					if(LibZen.IsVariableName(Token2.ParsedText, 0)) {
						Path += "${" + Token2.ParsedText + "}";
						HasStringExpr = true;
						TokenContext.GetTokenAndMoveForward();
						if(Token2.IsNextWhiteSpace()) {
							break;
						}
						continue;
					}
				}
				if(Token.EqualsText("{")) {
					HasStringExpr = true;
					FoundOpen = true;
				}
				if(Token.EqualsText("}")) {
					FoundOpen = false;
				}
				if(Token.EqualsText("~")) {
					ParsedText = System.getenv("HOME");
				}
				Path += ParsedText;
				if(!FoundOpen && Token.IsNextWhiteSpace()) {
					break;
				}
			}
		}
		if(!HasStringExpr) {
			return new ZStringNode(Token, Path);
		}
		Path = "\"" + Path + "\"";
		Path = Path.replaceAll("\\$\\{", "\" + ");
		Path = Path.replaceAll("\\}", " + \"");
		ZTokenContext LocalContext = new ZTokenContext(NameSpace, Path, Token.FileLine);
		return LocalContext.ParsePattern(NameSpace, "$Expression$", ZTokenContext.Required2);
	}

	private static ZNode CreateRedirectNode(ZNameSpace NameSpace, ZTokenContext TokenContext, String RedirectSymbol, boolean existTarget) {
		ZNode Node = new DShellCommandNode(new ZStringNode(new ZToken(0, RedirectSymbol, 0), RedirectSymbol));
		if(existTarget) {
			Node = TokenContext.AppendMatchedPattern(Node, NameSpace, "$CommandArg$", ZTokenContext.Required2);
		}
		return Node;
	}

	// <, >, >>, >&, 1>, 2>, 1>>, 2>>, &>, &>>
	public static ZNode MatchRedirect(ZNameSpace NameSpace, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetTokenAndMoveForward();
		String RedirectSymbol = Token.ParsedText;
		if(Token.EqualsText(">>") || Token.EqualsText("<")) {
			return CreateRedirectNode(NameSpace, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("&")) {
			ZToken Token2 = TokenContext.GetTokenAndMoveForward();
			if(Token2.EqualsText(">") || Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateRedirectNode(NameSpace, TokenContext, RedirectSymbol, true);
			}
		}
		else if(Token.EqualsText(">")) {
			ZToken Token2 = TokenContext.GetToken();
			if(Token2.EqualsText("&")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateRedirectNode(NameSpace, TokenContext, RedirectSymbol, true);
			}
			return CreateRedirectNode(NameSpace, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("1") || Token.EqualsText("2")) {
			ZToken Token2 = TokenContext.GetTokenAndMoveForward();
			if(Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateRedirectNode(NameSpace, TokenContext, RedirectSymbol, true);
			}
			else if(Token2.EqualsText(">")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateRedirectNode(NameSpace, TokenContext, RedirectSymbol, true);
			}
		}
		return null;
	}

	public static ZNode CreateNodeAndMatchNextOption(ZNameSpace NameSpace, ZTokenContext TokenContext, String OptionSymbol) {
		ZNode Node = new DShellCommandNode(new ZStringNode(new ZToken(0, OptionSymbol, 0), OptionSymbol));
		ZNode PipedNode = TokenContext.ParsePattern(NameSpace, "$SuffixOption$", ZTokenContext.Optional2);
		if(PipedNode != null) {
			((DShellCommandNode)Node).AppendPipedNextNode((DShellCommandNode)PipedNode);
		}
		if(!MatchStopToken(TokenContext)) {
			return new ZErrorNode(TokenContext.GetToken(), "not match stop token");
		}
		return Node;
	}

	public static ZNode MatchSuffixOption(ZNameSpace NameSpace, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetTokenAndMoveForward();
		String OptionSymbol = Token.ParsedText;
		if(Token.EqualsText(background)) {	// set background job
			return CreateNodeAndMatchNextOption(NameSpace, TokenContext, OptionSymbol);
		}
		return null;
	}

	private static ZToken GetJoinedCommandToken(ZTokenContext TokenContext) {
		ZToken Token = TokenContext.GetTokenAndMoveForward();
		String CommandSymbol = Token.ParsedText;
		long lineNum = Token.FileLine;
		while(!Token.IsNextWhiteSpace()) {
			if(MatchStopToken(TokenContext)) {
				break;
			}
			Token = TokenContext.GetTokenAndMoveForward();
			CommandSymbol += Token.ParsedText;
		}
		return new ZToken(0, CommandSymbol, lineNum);
	}

	private static ZFunc MatchDShell = LibNative.LoadMatchFunc(DShellGrammar.class, "MatchDShell");
	public static ZNode MatchDShell(ZNameSpace NameSpace, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken CommandToken = GetJoinedCommandToken(TokenContext);
		String Command = ((ZStringNode)NameSpace.GetSymbolNode(DShellGrammar.CommandSymbol(CommandToken.ParsedText))).StringValue;
		if(Command == null) {
			return new ZErrorNode(CommandToken, "undefined command symbol");
		}
		ZNode CommandNode = new DShellCommandNode(new ZStringNode(CommandToken, Command));
		TokenContext.SetBackTrack(false);
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
				ZNode pipedNode = TokenContext.ParsePattern(NameSpace, "$DShell$", ZTokenContext.Required2);
				if(pipedNode.IsErrorNode()) {
					return pipedNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)pipedNode);
			}
			// Match Redirect
			ZNode RedirectNode = TokenContext.ParsePattern(NameSpace, "$Redirect$", ZTokenContext.Optional2);
			if(RedirectNode != null) {
				((DShellCommandNode)CommandNode).AppendOptionNode((DShellCommandNode)RedirectNode);
				continue;
			}
			// Match Suffix Option
			ZNode OptionNode = TokenContext.ParsePattern(NameSpace, "$SuffixOption$", ZTokenContext.Optional2);
			if(OptionNode != null) {
				if(OptionNode instanceof ZErrorNode) {
					return OptionNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)OptionNode);
			}
			// Match Argument
			ZNode ArgNode = TokenContext.ParsePattern(NameSpace, "$CommandArg$", ZTokenContext.Optional2);
			if(ArgNode == null) {
				break;
			}
			CommandNode.Append(ArgNode);
		}
		return CommandNode;
	}

	public static ZNode MatchCatch(ZNameSpace NameSpace, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZNode CatchNode = new ZCatchNode();
		CatchNode = TokenContext.MatchNodeToken(CatchNode, NameSpace, "catch", ZTokenContext.Required2);
		CatchNode = TokenContext.MatchNodeToken(CatchNode, NameSpace, "(", ZTokenContext.Required2);
		CatchNode = TokenContext.AppendMatchedPattern(CatchNode, NameSpace, "$Identifier$", ZTokenContext.Required2);
		CatchNode = TokenContext.AppendMatchedPattern(CatchNode, NameSpace, "$TypeAnnotation$", ZTokenContext.Required2);
		CatchNode = TokenContext.MatchNodeToken(CatchNode, NameSpace, ")", ZTokenContext.Required2);
		CatchNode = TokenContext.AppendMatchedPattern(CatchNode, NameSpace, "$Block$", ZTokenContext.Required2);
		return CatchNode;
	}

	public static void ImportGrammar(ZNameSpace NameSpace, Class<?> Grammar) {
		LibNative.ImportGrammar(NameSpace, ZenGrammar.class.getName());
		NameSpace.AppendTokenFunc("#", LibNative.LoadTokenFunc(Grammar, "ShellCommentToken"));

		NameSpace.DefineSyntax("import", LibNative.LoadMatchFunc(Grammar, "MatchImport"));
		NameSpace.DefineSyntax("letenv", LibNative.LoadMatchFunc(Grammar, "MatchEnv"));
		NameSpace.DefineSyntax("$Env$", LibNative.LoadMatchFunc(Grammar, "MatchEnv"));
		NameSpace.DefineSyntax("command", LibNative.LoadMatchFunc(Grammar, "MatchCommand"));
		NameSpace.DefineSyntax("$Command$", LibNative.LoadMatchFunc(Grammar, "MatchCommand"));
		NameSpace.DefineSyntax("$CommandArg$", LibNative.LoadMatchFunc(Grammar, "MatchArgument"));
		NameSpace.DefineSyntax("$Redirect$", LibNative.LoadMatchFunc(Grammar, "MatchRedirect"));
		NameSpace.DefineSyntax("$SuffixOption$", LibNative.LoadMatchFunc(Grammar, "MatchSuffixOption"));
		NameSpace.DefineSyntax("$DShell$", MatchDShell);
		NameSpace.DefineSuffixSyntax("=~", ZenPrecedence.CStyleEquals, LibNative.LoadMatchFunc(ZenGrammar.class, "MatchComparator"));
		NameSpace.DefineSyntax("assert", LibNative.LoadMatchFunc(ZenGrammar.class, "MatchUnary"));
		NameSpace.DefineSyntax("log", LibNative.LoadMatchFunc(ZenGrammar.class, "MatchUnary"));
		NameSpace.DefineSyntax("$Catch$", LibNative.LoadMatchFunc(Grammar, "MatchCatch"));
		// prefix option
		// timeout
		setOptionalSymbol(NameSpace, timeout);
		// trace
		setOptionalSymbol(NameSpace, trace);
		// from BultinCommandMap
		ArrayList<String> symbolList = BuiltinCommand.getCommandSymbolList();
		for(String symbol : symbolList) {
			setOptionalSymbol(NameSpace, symbol);
		}
		NameSpace.Generator.AppendGrammarInfo("dshell0.1");
	}

	private static void setOptionalSymbol(ZNameSpace NameSpace, String symbol) {
		NameSpace.DefineSyntax(symbol, MatchDShell);
		NameSpace.SetGlobalSymbol(CommandSymbol(symbol), new ZStringNode(new ZToken(0, symbol, 0), symbol));
	}
}
