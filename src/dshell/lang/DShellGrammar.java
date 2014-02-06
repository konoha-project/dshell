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
import zen.parser.ZSource;
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

	private static void AppendCommand(ZNode ParentNode, String CommandPath, String CommandPrefix) {
		if(CommandPath.length() == 0) {
			return;
		}
		ZNameSpace NameSpace = ParentNode.GetNameSpace();
		int loc = CommandPath.lastIndexOf('/');
		String Command = CommandPath;
		if(loc != -1) {
			if(!Utils.isFileExecutable(CommandPath)) {	//FIXME: error report
				//NameSpace.Generator.Logger.Report(ZLogger.ErrorLevel, SourceToken, "not executable: " + CommandPath);
				return;
			}
			Command = CommandPath.substring(loc + 1);
		}
		else {
			if(!Utils.isUnixCommand(CommandPath)) {
				//NameSpace.Generator.Logger.Report(ZLogger.ErrorLevel, SourceToken, "unknown command: " + CommandPath);
				return;
			}
		}
		NameSpace.DefineSyntax(Command, MatchDShell);
		if(!CommandPrefix.equals(CommandPath)) {	//FIXME: check duplication
			NameSpace.DefineSyntax(CommandPrefix, MatchDShell);
		}
		NameSpace.SetLocalSymbol(CommandSymbol(Command), new ZStringNode(ParentNode, null, CommandPath));
	}

	public static boolean ShellCommentToken(ZSource Source) {
		while(Source.HasChar()) {
			char ch = Source.ParseChar();
			if(ch == '\n') {
				break;
			}
			Source.MoveNext();
		}
		return true;
	}

	public final static boolean IsNextWhiteSpace(ZToken Token) {
		char ch = Token.Source.SourceAt(Token.EndIndex);
		if(ch == ' ' || ch == '\t' || ch == '\n') {
			return true;
		}
		return false;
	}

	public static ZNode MatchImport(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		TokenContext.MoveNext();
		ZToken Token = TokenContext.GetToken();
		if(Token.EqualsText("command")) {
			return TokenContext.ParsePattern(ParentNode, "$Command$", ZTokenContext.Required);
		}
		if(Token.EqualsText("env")) {
			return TokenContext.ParsePattern(ParentNode, "$Env$", ZTokenContext.Required);
		}
		return null;
	}

	public static ZNode MatchEnv(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode LetNode = new ZLetNode(ParentNode);
		LetNode = TokenContext.MatchToken(LetNode, "env", ZTokenContext.Required);
		LetNode = TokenContext.MatchPattern(LetNode, ZNode.NameInfo, "$Name$", ZTokenContext.Required);
		LetNode.Set(ZNode.TypeInfo, ParentNode.GetNameSpace().GetTypeNode("String", null));
		String Name = ((ZLetNode)LetNode).Symbol;
		String Env = System.getenv(Name);
		Env = (Env == null) ? "" : Env;
		LetNode.Set(ZLetNode.InitValue, new ZStringNode(ParentNode, null, Env));
		return LetNode;
	}

	public static ZNode MatchCommand(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		String Command = "";
		boolean foundSlash = true;
		String KeySymbol = null;
		String ParsedText = null;
		TokenContext.MoveNext();
		while(TokenContext.HasNext()) {
			ZToken Token = TokenContext.GetToken();
			ParsedText = Token.GetText();
			if(foundSlash && !Token.EqualsText("/")) {
				foundSlash = false;
				KeySymbol = ParsedText;
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
			if(Token.EqualsText(";") || Token.IsIndent()) {
				break;
			}
			Command += ParsedText;
			if(IsNextWhiteSpace(Token)) {
				AppendCommand(ParentNode, Command, KeySymbol);
				Command = "";
				foundSlash = true;
			}
			TokenContext.MoveNext();
		}
		if(!Command.equals("")) {
			AppendCommand(ParentNode, Command, KeySymbol);
		}
		return new DShellDummyNode(ParentNode);
	}

	private static boolean MatchStopToken(ZTokenContext TokenContext) { // ;,)]}&&||
		ZToken Token = TokenContext.GetToken();
		if(!TokenContext.HasNext()) {
			return true;
		}
		if(Token.IsIndent() || Token.EqualsText(";")) {
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
		if(Token.toString().startsWith("\"")) {
			Path = Token.GetText();
			if(Path.indexOf("${") != -1 && Path.lastIndexOf("}") != -1) {
				HasStringExpr = true;
			}
			TokenContext.MoveNext();
		}
		if(Path == null) {
			boolean FoundOpen = false;
			Path = "";
			while(TokenContext.HasNext()) {
				Token = TokenContext.GetToken();
				String ParsedText = Token.GetText();
				if(Token.IsIndent() || (!FoundOpen && MatchStopToken(TokenContext))) {
					break;
				}
				TokenContext.MoveNext();
				if(Token.EqualsText("$")) {   // $HOME/hoge
					ZToken Token2 = TokenContext.GetToken();
					String ParsedText2 = Token2.GetText();
					if(LibZen.IsVariableName(ParsedText2, 0)) {
						Path += "${" + ParsedText2 + "}";
						HasStringExpr = true;
						TokenContext.MoveNext();
						if(IsNextWhiteSpace(Token2)) {
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
				if(!FoundOpen && IsNextWhiteSpace(Token)) {
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
		new ZTokenContext(TokenContext.Generator, NameSpace, Token.GetFileName(), Token.GetLineNumber(), Path);
		ZTokenContext LocalContext = new ZTokenContext(TokenContext.Generator, NameSpace, Token.GetFileName(), Token.GetLineNumber(), Path);
		return LocalContext.ParsePattern(new ZBlockNode(NameSpace), "$Expression$", ZTokenContext.Required);
	}

	private static ZNode CreateRedirectNode(ZNode ParentNode, ZTokenContext TokenContext, String RedirectSymbol, boolean existTarget) {
		ZNode Node = new DShellCommandNode(ParentNode);
		Node.Set(ZNode.AppendIndex, new ZStringNode(ParentNode, null, RedirectSymbol));
		if(existTarget) {
			Node = TokenContext.MatchPattern(Node, ZNode.AppendIndex, "$CommandArg$", ZTokenContext.Required);
		}
		return Node;
	}

	// <, >, >>, >&, 1>, 2>, 1>>, 2>>, &>, &>>
	public static ZNode MatchRedirect(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		String RedirectSymbol = Token.GetText();
		if(Token.EqualsText(">>") || Token.EqualsText("<")) {
			return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("&")) {
			ZToken Token2 = TokenContext.GetToken();
			TokenContext.MoveNext();
			if(Token2.EqualsText(">") || Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.GetText();
				return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
		}
		else if(Token.EqualsText(">")) {
			ZToken Token2 = TokenContext.GetToken();
			if(Token2.EqualsText("&")) {
				RedirectSymbol += Token2.GetText();
				return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
			return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("1") || Token.EqualsText("2")) {
			ZToken Token2 = TokenContext.GetToken();
			TokenContext.MoveNext();
			if(Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.GetText();
				return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
			else if(Token2.EqualsText(">")) {
				RedirectSymbol += Token2.GetText();
				return CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
		}
		return null;
	}

	public static ZNode CreateNodeAndMatchNextOption(ZNode ParentNode, ZTokenContext TokenContext, String OptionSymbol) {
		ZNode Node = new DShellCommandNode(ParentNode);
		Node.Set(ZNode.AppendIndex, new ZStringNode(ParentNode, null, OptionSymbol));
		ZNode PipedNode = TokenContext.ParsePattern(ParentNode, "$SuffixOption$", ZTokenContext.Optional);
		if(PipedNode != null) {
			((DShellCommandNode)Node).AppendPipedNextNode((DShellCommandNode)PipedNode);
		}
		if(!MatchStopToken(TokenContext)) {
			return new ZErrorNode(ParentNode, TokenContext.GetToken(), "not match stop token");
		}
		return Node;
	}

	public static ZNode MatchSuffixOption(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		String OptionSymbol = Token.GetText();
		if(Token.EqualsText(background)) {	// set background job
			return CreateNodeAndMatchNextOption(ParentNode, TokenContext, OptionSymbol);
		}
		return null;
	}

	private static ZToken GetJoinedCommandToken(ZTokenContext TokenContext) {
		ZToken Token = TokenContext.GetToken();
		String CommandSymbol = Token.GetText();
		TokenContext.MoveNext();
		while(!IsNextWhiteSpace(Token)) {
			if(MatchStopToken(TokenContext)) {
				break;
			}
			Token = TokenContext.GetToken();
			CommandSymbol += Token.GetText();
			TokenContext.MoveNext();
		}
		ZSource CommandSource = new ZSource(Token.GetFileName(), Token.GetLineNumber(), CommandSymbol, TokenContext);
		return new ZToken(CommandSource, 0, CommandSymbol.length());
	}

	private static ZFunc MatchDShell = LibNative.LoadMatchFunc(DShellGrammar.class, "MatchDShell");
	public static ZNode MatchDShell(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken CommandToken = GetJoinedCommandToken(TokenContext);
		String Command = ((ZStringNode)ParentNode.GetNameSpace().GetSymbolNode(DShellGrammar.CommandSymbol(CommandToken.GetText()))).StringValue;
		if(Command == null) {
			return new ZErrorNode(ParentNode, CommandToken, "undefined command symbol");
		}
		ZNode CommandNode = new DShellCommandNode(ParentNode);
		CommandNode.Set(ZNode.AppendIndex, new ZStringNode(ParentNode, CommandToken, Command));
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
				ZNode PipedNode = TokenContext.ParsePattern(ParentNode, "$DShell$", ZTokenContext.Required);
				if(PipedNode.IsErrorNode()) {
					return PipedNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)PipedNode);
			}
			// Match Redirect
			ZNode RedirectNode = TokenContext.ParsePattern(ParentNode, "$Redirect$", ZTokenContext.Optional);
			if(RedirectNode != null) {
				((DShellCommandNode)CommandNode).AppendOptionNode((DShellCommandNode)RedirectNode);
				continue;
			}
			// Match Suffix Option
			ZNode OptionNode = TokenContext.ParsePattern(ParentNode, "$SuffixOption$", ZTokenContext.Optional);
			if(OptionNode != null) {
				if(OptionNode instanceof ZErrorNode) {
					return OptionNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)OptionNode);
			}
			// Match Argument
			ZNode ArgNode = TokenContext.ParsePattern(ParentNode, "$CommandArg$", ZTokenContext.Optional);
			if(ArgNode == null) {
				break;
			}
			CommandNode.Set(ZNode.AppendIndex, ArgNode);
		}
		return CommandNode;
	}

	public static ZNode MatchDShellTry(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZNode TryNode = new DShellTryNode(ParentNode);
		TryNode = TokenContext.MatchToken(TryNode, "try", ZTokenContext.Required);
		TryNode = TokenContext.MatchPattern(TryNode, DShellTryNode.Try, "$Block$", ZTokenContext.Required);
		int count = 0;
		while(true) {
			if(TokenContext.IsNewLineToken("catch")) {
				TryNode = TokenContext.MatchPattern(TryNode, ZNode.AppendIndex, "$Catch$", ZTokenContext.Required);
				count = count + 1;
				continue;
			}
			if(TokenContext.MatchNewLineToken("finally")) {
				TryNode = TokenContext.MatchPattern(TryNode, DShellTryNode.Finally, "$Block$", ZTokenContext.Required);
				count = count + 1;
			}
			break;
		}
		if(count == 0 && !TryNode.IsErrorNode()) {
			return ((DShellTryNode)TryNode).AST[DShellTryNode.Try]; // no catch and finally
		}
		return TryNode;
	}

	public static ZNode MatchCatch(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZNode CatchNode = new ZCatchNode(ParentNode);
		CatchNode = TokenContext.MatchToken(CatchNode, "catch", ZTokenContext.Required);
		CatchNode = TokenContext.MatchToken(CatchNode, "(", ZTokenContext.Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, ZNode.NameInfo, "$Name$", ZTokenContext.Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, ZNode.TypeInfo, "$TypeAnnotation$", ZTokenContext.Required);
		CatchNode = TokenContext.MatchToken(CatchNode, ")", ZTokenContext.Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, ZCatchNode.Block, "$Block$", ZTokenContext.Required);
		return CatchNode;
	}

	public static ZNode MatchLocationDef(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {	//TODO: multiple host, ssh
		TokenContext.MoveNext();
		ZNode Node = TokenContext.ParsePattern(ParentNode, "$Identifier$", ZTokenContext.Required);
		if(!Node.IsErrorNode() && TokenContext.MatchToken("=")) {
			ZNode ValueNode = TokenContext.ParsePattern(ParentNode, "$StringLiteral$", ZTokenContext.Required);
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
		ZToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		ZStringNode KeyNode = new ZStringNode(ParentNode, null, location);
		DShellCommandNode Node = new DShellCommandNode(ParentNode);
		Node.Set(ZNode.AppendIndex, KeyNode);
		Node.Append(ParentNode.GetNameSpace().GetSymbolNode(Token.GetText()));
		ZNode PipedNode = TokenContext.ParsePattern(ParentNode, "$DShell$", ZTokenContext.Required);
		if(!PipedNode.IsErrorNode()) {
			return Node.AppendPipedNextNode((DShellCommandNode) PipedNode);
		}
		return null;
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
		NameSpace.SetGlobalSymbol(CommandSymbol(symbol), new ZStringNode(new ZBlockNode(NameSpace), null, symbol));
	}
}
