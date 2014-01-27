package dshell.lang;

import java.util.ArrayList;

import zen.ast.ZEmptyNode;
import zen.ast.ZErrorNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.deps.LibNative;
import zen.deps.LibZen;
import zen.lang.ZenGrammar;
import zen.parser.ZLogger;
import zen.parser.ZNameSpace;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;
import dshell.ast.DShellCommandNode;
import dshell.lib.BuiltinCommandMap;
import dshell.util.Utils;

public class DShellGrammar {
	// suffix option symbol
	public final static String background = "&";
	public final static String errorAction_raise = "--erroraction=raise";
	public final static String errorAction_trace = "--erroraction=trace";
	// builtin command symbol 
	public final static String timeout = "timeout";

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
		NameSpace.SetSymbol(Command, NameSpace.GetSyntaxPattern("$DShell$"), SourceToken);
		String CommandPrefix = KeyToken.ParsedText;
		if(!CommandPrefix.equals(SourceToken.ParsedText) && !NameSpace.HasSymbol(CommandPrefix)) {
			NameSpace.SetSymbol(CommandPrefix, NameSpace.GetSyntaxPattern("$DShell$"), KeyToken);
		}
		NameSpace.SetSymbol(DShellGrammar.CommandSymbol(Command), CommandPath, null);
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
			return TokenContext.ParsePattern(NameSpace, "$Command$", ZTokenContext.Required);
		}
		if(TokenContext.IsToken("env")) {
			return TokenContext.ParsePattern(NameSpace, "$Env$", ZTokenContext.Required);
		}
		return null;
	}

	public static ZNode MatchEnv(ZNameSpace NameSpace, ZTokenContext TokenContext, ZNode LeftNode) {
		TokenContext.GetTokenAndMoveForward();
		ZToken Token = TokenContext.GetTokenAndMoveForward();
		if(!LibZen.IsVariableName(Token.ParsedText, 0)) {
			return new ZErrorNode(Token, "name");
		}
		String Name = Token.ParsedText;
		String Env  = System.getenv(Name);
		if(TokenContext.MatchToken("=")) {
			if(Env == null) {
				ZNode ConstNode = TokenContext.ParsePattern(NameSpace, "$Expression$", ZTokenContext.Required);
				if(ConstNode.IsErrorNode()) {
					return ConstNode;
				}
				Env = ((ZStringNode)ConstNode).StringValue;
			}
		}
		if(Env == null) {
			return new ZErrorNode(Token, "undefined environment variable: " + Name);
		}
		NameSpace.SetSymbol(Name, Env, Token);
		return new ZEmptyNode(Token);
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
		if(ParsedText != null) {
			SourceToken = new ZToken(0, ParsedText, lineNum);
			AppendCommand(NameSpace, Command, KeyToken, SourceToken);
		}
		return new ZEmptyNode(SourceToken);
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
		return LocalContext.ParsePattern(NameSpace, "$Expression$", ZTokenContext.Required);
	}

	private static ZNode CreateNodeAndMatchNextRedirect(ZNameSpace NameSpace, ZTokenContext TokenContext, String RedirectSymbol, boolean existTarget) {
		ZNode Node = new DShellCommandNode(new ZStringNode(new ZToken(0, RedirectSymbol, 0), RedirectSymbol));
		if(existTarget) {
			Node = TokenContext.AppendMatchedPattern(Node, NameSpace, "$CommandArg$", ZTokenContext.Required);
		}
		ZNode PipedNode = TokenContext.ParsePattern(NameSpace, "$Redirect$", ZTokenContext.Optional);
		if(PipedNode != null) {
			((DShellCommandNode)Node).AppendPipedNextNode((DShellCommandNode)PipedNode);
		}
		return Node;
	}

	// >, >>, >&, 1>, 2>, 1>>, 2>>, &>, &>>, 1>&1, 1>&2, 2>&1, 2>&2, >&1, >&2
	public static ZNode MatchRedirect(ZNameSpace NameSpace, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetTokenAndMoveForward();
		String RedirectSymbol = Token.ParsedText;
		if(Token.EqualsText(">>")) {
			return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("&")) {
			ZToken Token2 = TokenContext.GetTokenAndMoveForward();
			if(Token2.EqualsText(">") || Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, true);
			}
		}
		else if(Token.EqualsText(">")) {
			ZToken Token2 = TokenContext.GetToken();
			if(Token2.EqualsText("&")) {
				RedirectSymbol += Token2.ParsedText;
				ZToken Token3 = TokenContext.GetTokenAndMoveForward();
				if(Token3.EqualsText("1") || Token3.EqualsText("2")) {
					RedirectSymbol += Token3.ParsedText;
					return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, false);
				}
				return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, true);
			}
			return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("1") || Token.EqualsText("2")) {
			ZToken Token2 = TokenContext.GetTokenAndMoveForward();
			if(Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, true);
			}
			else if(Token2.EqualsText(">")) {
				RedirectSymbol += Token2.ParsedText;
				ZToken Token3 = TokenContext.GetToken();
				if(Token3.EqualsText("&")) {
					RedirectSymbol += Token3.ParsedText;
					TokenContext.GetTokenAndMoveForward();
					ZToken Token4 = TokenContext.GetTokenAndMoveForward();
					if(Token4.EqualsText("1") || Token4.EqualsText("2")) {
						RedirectSymbol += Token4.ParsedText;
						return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, false);
					}
					return null;
				}
				return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, true);
			}
		}
		return null;
	}

	public static ZNode CreateNodeAndMatchNextOption(ZNameSpace NameSpace, ZTokenContext TokenContext, String OptionSymbol) {
		ZNode Node = new DShellCommandNode(new ZStringNode(new ZToken(0, OptionSymbol, 0), OptionSymbol));
		ZNode PipedNode = TokenContext.ParsePattern(NameSpace, "$SuffixOption$", ZTokenContext.Optional);
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
		if(Token.EqualsText("-") || Token.EqualsText("--")) {
			while(!Token.IsNextWhiteSpace()) {
				if(MatchStopToken(TokenContext)) {
					break;
				}
				Token = TokenContext.GetTokenAndMoveForward();
				OptionSymbol += Token.ParsedText;
			}
		}
		if(OptionSymbol.equals(errorAction_raise) || OptionSymbol.equals(errorAction_trace)) {
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

	public static ZNode MatchDShell(ZNameSpace NameSpace, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken CommandToken = GetJoinedCommandToken(TokenContext);
		String Command = (String)NameSpace.GetSymbol(DShellGrammar.CommandSymbol(CommandToken.ParsedText));
		if(Command == null) {
			return new ZErrorNode(CommandToken, "undefined command symbol");
		}
		ZNode CommandNode = new DShellCommandNode(new ZStringNode(CommandToken, Command));
		TokenContext.SetBackTrack(false);
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
				ZNode pipedNode = TokenContext.ParsePattern(NameSpace, "$DShell$", ZTokenContext.Required);
				if(pipedNode.IsErrorNode()) {
					return pipedNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)pipedNode);
			}
			// Match Redirect
			ZNode RedirectNode = TokenContext.ParsePattern(NameSpace, "$Redirect$", ZTokenContext.Optional);
			if(RedirectNode != null) {
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)RedirectNode);
			}
			// Match Suffix Option
			ZNode OptionNode = TokenContext.ParsePattern(NameSpace, "$SuffixOption$", ZTokenContext.Optional);
			if(OptionNode != null) {
				if(OptionNode instanceof ZErrorNode) {
					return OptionNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)OptionNode);
			}
			// Match Argument
			ZNode ArgNode = TokenContext.ParsePattern(NameSpace, "$CommandArg$", ZTokenContext.Optional);
			if(ArgNode == null) {
				break;
			}
			CommandNode.Append(ArgNode);
		}
		return CommandNode;
	}

	public static void ImportGrammar(ZNameSpace NameSpace, Class<?> Grammar) {
		LibNative.ImportGrammar(NameSpace, ZenGrammar.class.getName());
		NameSpace.AppendTokenFunc("#", LibNative.LoadTokenFunc(Grammar, "ShellCommentToken"));

		NameSpace.AppendSyntax("import", LibNative.LoadMatchFunc(Grammar, "MatchImport"));
		NameSpace.AppendSyntax("letenv", LibNative.LoadMatchFunc(Grammar, "MatchEnv"));
		NameSpace.AppendSyntax("$Env$", LibNative.LoadMatchFunc(Grammar, "MatchEnv"));
		NameSpace.AppendSyntax("command", LibNative.LoadMatchFunc(Grammar, "MatchCommand"));
		NameSpace.AppendSyntax("$Command$", LibNative.LoadMatchFunc(Grammar, "MatchCommand"));
		NameSpace.AppendSyntax("$CommandArg$", LibNative.LoadMatchFunc(Grammar, "MatchArgument"));
		NameSpace.AppendSyntax("$Redirect$", LibNative.LoadMatchFunc(Grammar, "MatchRedirect"));
		NameSpace.AppendSyntax("$SuffixOption$", LibNative.LoadMatchFunc(Grammar, "MatchSuffixOption"));
		NameSpace.AppendSyntax("$DShell$", LibNative.LoadMatchFunc(Grammar, "MatchDShell"));
		// builtin command
		// timeout
		NameSpace.SetSymbol(timeout, NameSpace.GetSyntaxPattern("$DShell$"), new ZToken(0, timeout, 0));
		NameSpace.SetSymbol(CommandSymbol(timeout), timeout, null);
		// from BultinCommandMap
		ArrayList<String> symbolList = BuiltinCommandMap.getCommandSymbolList();
		for(String symbol : symbolList) {
			NameSpace.SetSymbol(symbol, NameSpace.GetSyntaxPattern("$DShell$"), new ZToken(0, symbol, 0));
			NameSpace.SetSymbol(CommandSymbol(symbol), symbol, null);
		}
		NameSpace.Generator.AppendGrammarInfo("dshell0.1");
	}
}
