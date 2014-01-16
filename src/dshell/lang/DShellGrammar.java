package dshell.lang;

import dshell.ast.DShellCommandNode;
import dshell.util.Utils;

import zen.ast.ZenEmptyNode;
import zen.ast.ZenErrorNode;
import zen.ast.ZenNode;
import zen.ast.ZenStringNode;
import zen.deps.LibNative;
import zen.deps.LibZen;
import zen.lang.ZenGrammar;
import zen.parser.ZenNameSpace;
import zen.parser.ZenToken;
import zen.parser.ZenTokenContext;

public class DShellGrammar {
	private final static String FileOperators = "-d -e -f -r -w -x";

	private static String CommandSymbol(String Symbol) {
		return "__$" + Symbol;
	}

	private static void AppendCommand(ZenNameSpace NameSpace, String CommandPath, ZenToken KeyToken, ZenToken SourceToken) {
		if(CommandPath.length() == 0) {
			return;
		}
		int loc = CommandPath.lastIndexOf('/');
		String Command = CommandPath;
		if(loc != -1) {
			if(!Utils.isFileExecutable(CommandPath)) {
				System.err.println("not executable: " + CommandPath); //FIXME: error report
				return;
			}
			Command = CommandPath.substring(loc + 1);
		}
		else {
			if(!Utils.isUnixCommand(CommandPath)) {
				System.err.println("unknown command: " + CommandPath); //FIXME: error report
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

	public static long ShellCommentToken(ZenTokenContext TokenContext, String SourceText, long pos) {
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
		return ZenTokenContext.MismatchedPosition;
	}

	public static ZenNode MatchEnv(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		TokenContext.GetTokenAndMoveForward();
		ZenToken Token = TokenContext.GetTokenAndMoveForward();
		if(!LibZen.IsVariableName(Token.ParsedText, 0)) {
			return new ZenErrorNode(Token, "name");
		}
		String Name = Token.ParsedText;
		String Env  = System.getenv(Name);
		if(TokenContext.MatchToken("=")) {
			if(Env == null) {
				ZenNode ConstNode = TokenContext.ParsePattern(NameSpace, "$Expression$", ZenTokenContext.Required);
				if(ConstNode.IsErrorNode()) {
					return ConstNode;
				}
				Env = ((ZenStringNode)ConstNode).Value;
			}
		}
		if(Env == null) {
			return new ZenErrorNode(Token, "undefined environment variable: " + Name);
		}
		NameSpace.SetSymbol(Name, Env, Token);
		return new ZenEmptyNode(Token);
	}

	public static ZenNode MatchCommand(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		String Command = "";
		boolean foundSlash = true;
		ZenToken KeyToken = null;
		long lineNum = TokenContext.GetToken().FileLine;
		ZenToken SourceToken = null;
		String ParsedText = null;
		TokenContext.GetTokenAndMoveForward();
		while(TokenContext.HasNext()) {
			ZenToken Token = TokenContext.GetTokenAndMoveForward();
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
				ParsedText = null;
				break;
			}
			Command += ParsedText;
			if(Token.IsNextWhiteSpace()) {
				SourceToken = new ZenToken(0, ParsedText, lineNum);
				AppendCommand(NameSpace, Command, KeyToken, SourceToken);
				Command = "";
			}
		}
		if(ParsedText != null) {
			SourceToken = new ZenToken(0, ParsedText, lineNum);
			AppendCommand(NameSpace, Command, KeyToken, SourceToken);
		}
		return new ZenEmptyNode(SourceToken);
	}

	public static ZenNode MatchFileOperator(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		return null;
	}

	private static boolean MatchStopToken(ZenTokenContext TokenContext) { // ;,)]}&&||
		ZenToken Token = TokenContext.GetToken();
		if(Token.IsIndent() || Token.IsDelim()) {
			return true;
		}
		if(Token.EqualsText(",") || Token.EqualsText(")") || Token.EqualsText("]") || 
				Token.EqualsText("}") || Token.EqualsText("&&") || Token.EqualsText("||")) {
			return true;
		}
		return false;
	}

	public static ZenNode MatchArgument(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		if(MatchStopToken(TokenContext)) {
			return null;
		}
		ZenToken Token = TokenContext.GetToken();
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
					ZenToken Token2 = TokenContext.GetToken();
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
			return new ZenStringNode(Token, Path);
		}
		Path = "\"" + Path + "\"";
		Path = Path.replaceAll("\\$\\{", "\" + (");
		Path = Path.replaceAll("\\}", ") + \"");
		ZenTokenContext LocalContext = new ZenTokenContext(NameSpace, Path, Token.FileLine);
		return LocalContext.ParsePattern(NameSpace, "$Expression$", ZenTokenContext.Required);
	}

	private static ZenNode CreateNodeAndMatchNextRedirect(ZenNameSpace NameSpace, ZenTokenContext TokenContext, String RedirectSymbol, boolean existTarget) {
		ZenNode Node = new DShellCommandNode(new ZenStringNode(new ZenToken(0, RedirectSymbol, 0), RedirectSymbol));
		if(existTarget) {
			Node = TokenContext.AppendMatchedPattern(Node, NameSpace, "$Argument$", ZenTokenContext.Required);
		}
		ZenNode PipedNode = TokenContext.ParsePattern(NameSpace, "$Redirect$", ZenTokenContext.Optional);
		if(PipedNode != null) {
			((DShellCommandNode)Node).AppendPipedNextNode((DShellCommandNode)PipedNode);
		}
		return Node;
	}

	// >, >>, >&, 1>, 2>, 1>>, 2>>, &>, &>>, 1>&1, 1>&2, 2>&1, 2>&2, >&1, >&2
	public static ZenNode MatchRedirect(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		ZenToken Token = TokenContext.GetTokenAndMoveForward();
		String RedirectSymbol = Token.ParsedText;
		if(Token.EqualsText(">>")) {
			return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("&")) {
			ZenToken Token2 = TokenContext.GetTokenAndMoveForward();
			if(Token2.EqualsText(">") || Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, true);
			}
		}
		else if(Token.EqualsText(">")) {
			ZenToken Token2 = TokenContext.GetToken();
			if(Token2.EqualsText("&")) {
				RedirectSymbol += Token2.ParsedText;
				ZenToken Token3 = TokenContext.GetTokenAndMoveForward();
				if(Token3.EqualsText("1") || Token3.EqualsText("2")) {
					RedirectSymbol += Token3.ParsedText;
					return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, false);
				}
				return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, true);
			}
			return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("1") || Token.EqualsText("2")) {
			ZenToken Token2 = TokenContext.GetTokenAndMoveForward();
			if(Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateNodeAndMatchNextRedirect(NameSpace, TokenContext, RedirectSymbol, true);
			}
			else if(Token2.EqualsText(">")) {
				RedirectSymbol += Token2.ParsedText;
				ZenToken Token3 = TokenContext.GetToken();
				if(Token3.EqualsText("&")) {
					RedirectSymbol += Token3.ParsedText;
					TokenContext.GetTokenAndMoveForward();
					ZenToken Token4 = TokenContext.GetTokenAndMoveForward();
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

	public static ZenNode CreateNodeAndMatchNextOption(ZenNameSpace NameSpace, ZenTokenContext TokenContext, String OptionSymbol) {
		ZenNode Node = new DShellCommandNode(new ZenStringNode(new ZenToken(0, OptionSymbol, 0), OptionSymbol));
		ZenNode PipedNode = TokenContext.ParsePattern(NameSpace, "$SuffixOption$", ZenTokenContext.Optional);
		if(PipedNode != null) {
			((DShellCommandNode)Node).AppendPipedNextNode((DShellCommandNode)PipedNode);
		}
		return Node;
	}

	public static ZenNode MatchSuffixOption(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		ZenToken Token = TokenContext.GetTokenAndMoveForward();
		String OptionSymbol = Token.ParsedText;
		if(Token.EqualsText("&")) {	// set background job
			return CreateNodeAndMatchNextOption(NameSpace, TokenContext, OptionSymbol);
		}
		return null;
	}

	private static ZenToken GetJoinedCommandToken(ZenTokenContext TokenContext) {
		ZenToken Token = TokenContext.GetTokenAndMoveForward();
		String ParsedText = Token.ParsedText;
		long lineNum = Token.FileLine;
		while(!Token.IsNextWhiteSpace()) {
			if(!TokenContext.HasNext() || MatchStopToken(TokenContext)) {
				break;
			}
			Token = TokenContext.GetTokenAndMoveForward();
			ParsedText += Token.ParsedText;
		}
		return new ZenToken(0, ParsedText, lineNum);
	}

	public static ZenNode MatchDShell(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		ZenToken CommandToken = GetJoinedCommandToken(TokenContext);
		String Command = (String)NameSpace.GetSymbol(DShellGrammar.CommandSymbol(CommandToken.ParsedText));
		if(Command == null) {
			return new ZenErrorNode(CommandToken, "undefined command symbol");
		}
		ZenNode CommandNode = new DShellCommandNode(new ZenStringNode(CommandToken, Command));
		TokenContext.SetBackTrack(false);
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
				ZenNode pipedNode = TokenContext.ParsePattern(NameSpace, "$DShell$", ZenTokenContext.Required);
				if(pipedNode.IsErrorNode()) {
					return pipedNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)pipedNode);
			}
			// Match Redirect
			ZenNode RedirectNode = TokenContext.ParsePattern(NameSpace, "$Redirect$", ZenTokenContext.Optional);
			if(RedirectNode != null) {
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)RedirectNode);
			}
			// Match Suffix Option
			ZenNode OptionNode = TokenContext.ParsePattern(NameSpace, "$SuffixOption$", ZenTokenContext.Optional);
			if(OptionNode != null) {
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)OptionNode);
			}
			// Match Argument
			ZenNode ArgNode = TokenContext.ParsePattern(NameSpace, "$Argument$", ZenTokenContext.Optional);
			if(ArgNode == null) {
				break;
			}
			CommandNode.Append(ArgNode);
		}
		return CommandNode;
	}

	public static void ImportGrammar(ZenNameSpace NameSpace, Class<?> Grammar) {
		NameSpace.AppendTokenFunc("#", LibNative.LoadTokenFunc(Grammar, "ShellCommentToken")); 
		
		NameSpace.AppendSyntax("letenv", LibNative.LoadMatchFunc(Grammar, "MatchEnv"));
		NameSpace.AppendSyntax("command", LibNative.LoadMatchFunc(Grammar, "MatchCommand"));
		NameSpace.AppendSyntax("-", LibNative.LoadMatchFunc(Grammar, "MatchFileOperator"));
		NameSpace.AppendSyntax("$Argument$", LibNative.LoadMatchFunc(Grammar, "MatchArgument"));
		NameSpace.AppendSyntax("$Redirect$", LibNative.LoadMatchFunc(Grammar, "MatchRedirect"));
		NameSpace.AppendSyntax("$SuffixOption$", LibNative.LoadMatchFunc(Grammar, "MatchSuffixOption"));
		NameSpace.AppendSyntax("$DShell$", LibNative.LoadMatchFunc(Grammar, "MatchDShell"));
		NameSpace.Generator.SetGrammarInfo("dshell0.1");
	}
}
