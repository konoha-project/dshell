package dshell.lang;

import java.util.ArrayList;

import zen.ast.ZBlockNode;
import zen.ast.ZCatchNode;
import zen.ast.ZErrorNode;
import zen.ast.ZGetNameNode;
import zen.ast.ZLetNode;
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
import dshell.ast.DShellTryNode;
import dshell.lib.BuiltinCommand;
import dshell.util.Utils;

public class DShellGrammar {
	// suffix option symbol
	public final static String background = "&";
	// prefix option symbol 
	public final static String timeout = "timeout";
	public final static String trace = "trace";
	public final static String location = "location";

	private static String CommandSymbol(String Symbol) {
		return "__$" + Symbol;
	}

	private static void AppendCommand(ZNode ParentNode, String CommandPath, ZToken KeyToken, ZToken SourceToken) {
		if(CommandPath.length() == 0) {
			return;
		}
		ZNameSpace NameSpace = ParentNode.GetNameSpace();
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
		NameSpace.SetLocalSymbol(CommandSymbol(Command), new ZStringNode(ParentNode, SourceToken, CommandPath));
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

	public static ZNode MatchImport(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		TokenContext.GetTokenAndMoveForward();
		if(TokenContext.IsToken("command")) {
			return TokenContext.ParsePattern(ParentNode, "$Command$", ZTokenContext.Required2);
		}
		if(TokenContext.IsToken("env")) {
			return TokenContext.ParsePattern(ParentNode, "$Env$", ZTokenContext.Required2);
		}
		return null;
	}

	public static ZNode MatchEnv(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode LetNode = new ZLetNode(ParentNode);
		LetNode = TokenContext.MatchNodeToken(LetNode, "env", ZTokenContext.Required2);
		LetNode = TokenContext.AppendMatchedPattern(LetNode, "$Identifier$", ZTokenContext.Required2);
		LetNode.Append(null);
		LetNode.Append(ParentNode.GetNameSpace().GetTypeNode("String", new ZToken(0, "String", 0)));
		String Name = ((ZLetNode)LetNode).Symbol;
		String Env = System.getenv(Name);
		Env = (Env == null) ? "" : Env;
		LetNode.Append(new ZStringNode(ParentNode, new ZToken(0, Env, 0), Env));
		return LetNode;
	}

	public static ZNode MatchCommand(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
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
				AppendCommand(ParentNode, Command, KeyToken, SourceToken);
				Command = "";
			}
		}
		if(!Command.equals("")) {
			SourceToken = new ZToken(0, Command, lineNum);
			AppendCommand(ParentNode, Command, KeyToken, SourceToken);
		}
		return new DShellDummyNode(ParentNode);
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

	public static ZNode MatchArgument(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
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
			return new ZStringNode(ParentNode, Token, Path);
		}
		Path = "\"" + Path + "\"";
		Path = Path.replaceAll("\\$\\{", "\" + ");
		Path = Path.replaceAll("\\}", " + \"");
		ZNameSpace NameSpace = ParentNode.GetNameSpace();
		ZTokenContext LocalContext = new ZTokenContext(TokenContext.Generator, NameSpace, Path, Token.FileLine);
		return LocalContext.ParsePattern(new ZBlockNode(NameSpace), "$Expression$", ZTokenContext.Required2);
	}

	private static ZNode CreateRedirectNode(ZNode ParentNode, ZTokenContext TokenContext, String RedirectSymbol, boolean existTarget) {
		ZNode Node = new DShellCommandNode(ParentNode, new ZStringNode(ParentNode, new ZToken(0, RedirectSymbol, 0), RedirectSymbol));
		if(existTarget) {
			Node = TokenContext.AppendMatchedPattern(Node, "$CommandArg$", ZTokenContext.Required2);
		}
		return Node;
	}

	// <, >, >>, >&, 1>, 2>, 1>>, 2>>, &>, &>>
	public static ZNode MatchRedirect(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetTokenAndMoveForward();
		String RedirectSymbol = Token.ParsedText;
		if(Token.EqualsText(">>") || Token.EqualsText("<")) {
			return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("&")) {
			ZToken Token2 = TokenContext.GetTokenAndMoveForward();
			if(Token2.EqualsText(">") || Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
		}
		else if(Token.EqualsText(">")) {
			ZToken Token2 = TokenContext.GetToken();
			if(Token2.EqualsText("&")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
			return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("1") || Token.EqualsText("2")) {
			ZToken Token2 = TokenContext.GetTokenAndMoveForward();
			if(Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
			else if(Token2.EqualsText(">")) {
				RedirectSymbol += Token2.ParsedText;
				return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
		}
		return null;
	}

	public static ZNode CreateNodeAndMatchNextOption(ZNode ParentNode, ZTokenContext TokenContext, String OptionSymbol) {
		ZNode Node = new DShellCommandNode(ParentNode, new ZStringNode(ParentNode, new ZToken(0, OptionSymbol, 0), OptionSymbol));
		ZNode PipedNode = TokenContext.ParsePattern(ParentNode, "$SuffixOption$", ZTokenContext.Optional2);
		if(PipedNode != null) {
			((DShellCommandNode)Node).AppendPipedNextNode((DShellCommandNode)PipedNode);
		}
		if(!MatchStopToken(TokenContext)) {
			return new ZErrorNode(ParentNode, TokenContext.GetToken(), "not match stop token");
		}
		return Node;
	}

	public static ZNode MatchSuffixOption(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetTokenAndMoveForward();
		String OptionSymbol = Token.ParsedText;
		if(Token.EqualsText(background)) {	// set background job
			return CreateNodeAndMatchNextOption(ParentNode, TokenContext, OptionSymbol);
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
	public static ZNode MatchDShell(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken CommandToken = GetJoinedCommandToken(TokenContext);
		String Command = ((ZStringNode)ParentNode.GetNameSpace().GetSymbolNode(DShellGrammar.CommandSymbol(CommandToken.ParsedText))).StringValue;
		if(Command == null) {
			return new ZErrorNode(ParentNode, CommandToken, "undefined command symbol");
		}
		ZNode CommandNode = new DShellCommandNode(ParentNode, new ZStringNode(ParentNode, CommandToken, Command));
		TokenContext.SetBackTrack(false);
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
				ZNode pipedNode = TokenContext.ParsePattern(ParentNode, "$DShell$", ZTokenContext.Required2);
				if(pipedNode.IsErrorNode()) {
					return pipedNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)pipedNode);
			}
			// Match Redirect
			ZNode RedirectNode = TokenContext.ParsePattern(ParentNode, "$Redirect$", ZTokenContext.Optional2);
			if(RedirectNode != null) {
				((DShellCommandNode)CommandNode).AppendOptionNode((DShellCommandNode)RedirectNode);
				continue;
			}
			// Match Suffix Option
			ZNode OptionNode = TokenContext.ParsePattern(ParentNode, "$SuffixOption$", ZTokenContext.Optional2);
			if(OptionNode != null) {
				if(OptionNode instanceof ZErrorNode) {
					return OptionNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)OptionNode);
			}
			// Match Argument
			ZNode ArgNode = TokenContext.ParsePattern(ParentNode, "$CommandArg$", ZTokenContext.Optional2);
			if(ArgNode == null) {
				break;
			}
			CommandNode.Append(ArgNode);
		}
		return CommandNode;
	}

	public static ZNode MatchDShellTry(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZNode TryNode = new DShellTryNode(ParentNode);
		TryNode = TokenContext.MatchNodeToken(TryNode, "try", ZTokenContext.Required2);
		TryNode = TokenContext.AppendMatchedPattern(TryNode, "$Block$", ZTokenContext.Required2);
		int count = 0;
		while(true) {
			if(TokenContext.IsNewLineToken("catch")) {
				TryNode = TokenContext.AppendMatchedPattern(TryNode, "$Catch$", ZTokenContext.Required2);
				count = count + 1;
				continue;
			}
			if(TokenContext.MatchNewLineToken("finally")) {
				TryNode = TokenContext.AppendMatchedPattern(TryNode, "$Block$", ZTokenContext.Required2);
				count = count + 1;
			}
			break;
		}
		if(count == 0 && !TryNode.IsErrorNode()) {
			return ((DShellTryNode)TryNode).TryNode; // no catch and finally
		}
		return TryNode;
	}

	public static ZNode MatchCatch(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZNode CatchNode = new ZCatchNode(ParentNode);
		CatchNode = TokenContext.MatchNodeToken(CatchNode, "catch", ZTokenContext.Required2);
		CatchNode = TokenContext.MatchNodeToken(CatchNode, "(", ZTokenContext.Required2);
		CatchNode = TokenContext.AppendMatchedPattern(CatchNode, "$Identifier$", ZTokenContext.Required2);
		CatchNode = TokenContext.AppendMatchedPattern(CatchNode, "$TypeAnnotation$", ZTokenContext.Required2);
		CatchNode = TokenContext.MatchNodeToken(CatchNode, ")", ZTokenContext.Required2);
		CatchNode = TokenContext.AppendMatchedPattern(CatchNode, "$Block$", ZTokenContext.Required2);
		return CatchNode;
	}

	public static ZNode MatchLocationDef(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {	//TODO: multiple host, ssh
		TokenContext.GetTokenAndMoveForward();
		ZNode Node = TokenContext.ParsePattern(ParentNode, "$Identifier$", ZTokenContext.Required2);
		if(!Node.IsErrorNode() && TokenContext.MatchToken("=")) {
			ZNode ValueNode = TokenContext.ParsePattern(ParentNode, "$StringLiteral$", ZTokenContext.Required2);
			if(!ValueNode.IsErrorNode()) {
				String NameSymbol = ((ZGetNameNode)Node).VarName;
				ParentNode.GetNameSpace().DefineSyntax(NameSymbol, LibNative.LoadMatchFunc(DShellGrammar.class, "MatchLocation"));
				ParentNode.GetNameSpace().SetGlobalSymbol(NameSymbol, (ZStringNode)ValueNode);
				return new DShellDummyNode(ParentNode);
			}
		}
		return null;
	}

	public static ZNode MatchLocation(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetTokenAndMoveForward();
		ZStringNode KeyNode = new ZStringNode(ParentNode, new ZToken(0, location, TokenContext.ParsingLine), location);
		DShellCommandNode Node = new DShellCommandNode(ParentNode, KeyNode);
		Node.Append(ParentNode.GetNameSpace().GetSymbolNode(Token.ParsedText));
		ZNode PipedNode = TokenContext.ParsePattern(ParentNode, "$DShell$", ZTokenContext.Required2);
		return Node.AppendPipedNextNode((DShellCommandNode) PipedNode);
	}

	public static void ImportGrammar(ZNameSpace NameSpace, Class<?> Grammar) {
		LibNative.ImportGrammar(NameSpace, ZenGrammar.class.getName());
		NameSpace.AppendTokenFunc("#", LibNative.LoadTokenFunc(Grammar, "ShellCommentToken"));

		NameSpace.DefineStatement("import", LibNative.LoadMatchFunc(Grammar, "MatchImport"));
		NameSpace.DefineSyntax("$Env$", LibNative.LoadMatchFunc(Grammar, "MatchEnv"));
		NameSpace.DefineStatement("command", LibNative.LoadMatchFunc(Grammar, "MatchCommand"));
		NameSpace.DefineSyntax("$Command$", LibNative.LoadMatchFunc(Grammar, "MatchCommand"));
		NameSpace.DefineSyntax("$CommandArg$", LibNative.LoadMatchFunc(Grammar, "MatchArgument"));
		NameSpace.DefineSyntax("$Redirect$", LibNative.LoadMatchFunc(Grammar, "MatchRedirect"));
		NameSpace.DefineSyntax("$SuffixOption$", LibNative.LoadMatchFunc(Grammar, "MatchSuffixOption"));
		NameSpace.DefineSyntax("$DShell$", MatchDShell);
		NameSpace.DefineSuffixSyntax("=~", ZenPrecedence.CStyleEquals, LibNative.LoadMatchFunc(ZenGrammar.class, "MatchComparator"));
		NameSpace.DefineSuffixSyntax("!~", ZenPrecedence.CStyleEquals, LibNative.LoadMatchFunc(ZenGrammar.class, "MatchComparator"));
		NameSpace.DefineSyntax("assert", LibNative.LoadMatchFunc(ZenGrammar.class, "MatchUnary"));
		NameSpace.DefineSyntax("log", LibNative.LoadMatchFunc(ZenGrammar.class, "MatchUnary"));
		NameSpace.DefineStatement("try", LibNative.LoadMatchFunc(Grammar, "MatchDShellTry"));
		NameSpace.DefineSyntax("$Catch$", LibNative.LoadMatchFunc(Grammar, "MatchCatch"));
		NameSpace.DefineSyntax(location, LibNative.LoadMatchFunc(Grammar, "MatchLocationDef"));
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
		NameSpace.SetGlobalSymbol(CommandSymbol(symbol), new ZStringNode(new ZBlockNode(NameSpace), new ZToken(0, symbol, 0), symbol));
	}
}
