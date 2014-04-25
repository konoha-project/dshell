package dshell.grammar;

import java.util.ArrayList;

import dshell.ast.sugar.DShellExportEnvNode;
import dshell.ast.sugar.DShellImportEnvNode;
import dshell.lib.BuiltinCommand;
import dshell.lib.RuntimeContext;
import dshell.lib.Utils;
import dshell.ast.sugar.CommandNode;
import dshell.ast.sugar.ArgumentNode;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.EmptyNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.BPatternToken;
import libbun.parser.BSourceContext;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.parser.LibBunGamma;
import libbun.parser.LibBunSyntax;
import libbun.type.BType;
import libbun.util.BArray;
import libbun.util.BMatchFunction;
import libbun.util.BTokenFunction;
import libbun.util.LibBunSystem;
import libbun.util.Var;

// Token
class ShellStyleCommentTokenFunc extends BTokenFunction {
	@Override public boolean Invoke(BSourceContext SourceContext) {
		while(SourceContext.HasChar()) {
			char ch = SourceContext.GetCurrentChar();
			if(ch == '\n') {
				break;
			}
			SourceContext.MoveNext();
		}
		return true;
	}
}

class CommandSymbolTokenFunc extends BTokenFunction {
	@Override public boolean Invoke(BSourceContext SourceContext) {
		int StartIndex = SourceContext.GetPosition();
		StringBuilder SymbolBuilder = new StringBuilder();
		while(SourceContext.HasChar()) {
			char ch = SourceContext.GetCurrentChar();
			if(!LibBunSystem._IsDigitOrLetter(ch) && ch != '-' && ch != '+' && ch != '_') {
				break;
			}
			SymbolBuilder.append(ch);
			SourceContext.MoveNext();
		}
		if(RuntimeContext.getContext().commandScope.isCommand(SymbolBuilder.toString())) {
			SourceContext.Tokenize(CommandSymbolPatternFunc._PatternName, StartIndex, SourceContext.GetPosition());
			return true;
		}
		return false;
	}
}

// Syntax Pattern
class ImportPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		TokenContext.MoveNext();
		BToken Token = TokenContext.GetToken();
		if(Token.EqualsText("command")) {
			return TokenContext.ParsePattern(ParentNode, ImportCommandPatternFunc._PatternName, BTokenContext._Required);
		}
		if(Token.EqualsText("env")) {
			return TokenContext.ParsePattern(ParentNode, ImportEnvPatternFunc.PatternName, BTokenContext._Required);
		}
		return null;
	}
}

class ImportCommandPatternFunc extends BMatchFunction {
	public final static String _PatternName = "$ImportCommand$";

	private BToken ToCommandToken(ArrayList<BToken> TokenList) {
		if(TokenList.isEmpty()) {
			return null;
		}
		int StartIndex = TokenList.get(0).StartIndex;
		int EndIndex = TokenList.get(TokenList.size() - 1).EndIndex;
		BToken CommandToken = new BToken(TokenList.get(0).Source, StartIndex, EndIndex);
		TokenList.clear();
		return CommandToken;
	}

	private void CheckDuplicationAndSetCommand(LibBunGamma Gamma, String Command, String CommandPath) {
		LibBunSyntax Syntax = Gamma.GetSyntaxPattern(Command);
		if(Syntax != null) {
			if(LibBunSystem.DebugMode) {
				System.err.println("found duplicated syntax pattern: " + Syntax);
			}
		}
		else if(!RuntimeContext.getContext().commandScope.setCommand(Command, CommandPath)) {
			if(LibBunSystem.DebugMode) {
				System.err.println("found duplicated symbol: " + Command);
			}
		}
	}

	private void SetCommandSymbol(BNode ParentNode, BTokenContext TokenContext, ArrayList<BToken> TokenList) {
		BToken CommandToken = this.ToCommandToken(TokenList);
		if(CommandToken == null) {
			return;
		}
		String CommandPath = Utils.resolveHome(CommandToken.GetText());
		LibBunGamma Gamma = ParentNode.GetGamma();
		int loc = CommandPath.lastIndexOf('/');
		String Command = CommandPath;
		if(loc != -1) {
			if(!Utils.isFileExecutable(CommandPath)) {
				System.err.println("[warning] unknown command: " + CommandPath);
				return;
			}
			Command = CommandPath.substring(loc + 1);
		}
		else {
			String FullPath = Utils.getCommandFromPath(CommandPath);
			if(FullPath == null) {
				System.err.println("[warning] unknown command: " + CommandPath);
				return;
			}
			CommandPath = FullPath;
		}
		this.CheckDuplicationAndSetCommand(Gamma, Command, CommandPath);
	}

	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		ArrayList<BToken> TokenList = new ArrayList<BToken>();
		TokenContext.MoveNext();
		while(TokenContext.HasNext()) {
			BToken Token = TokenContext.GetToken();
			if(Token.EqualsText(";") || Token.IsIndent()) {
				break;
			}
			if(!Token.EqualsText(",")) {
				TokenList.add(Token);
			}
			if(Token.IsNextWhiteSpace()) {
				this.SetCommandSymbol(ParentNode, TokenContext, TokenList);
			}
			TokenContext.MoveNext();
		}
		this.SetCommandSymbol(ParentNode, TokenContext, TokenList);
		return new EmptyNode(ParentNode);
	}
}

class CommandSymbolPatternFunc extends BMatchFunction {
	public final static String _PatternName = "$CommandSymbol$";

	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BToken CommandToken = TokenContext.GetToken(BTokenContext._MoveNext);
		String Command = RuntimeContext.getContext().commandScope.getCommand(CommandToken.GetText());
		if(Command == null) {
			return new ErrorNode(ParentNode, CommandToken, "undefined command symbol");
		}
		CommandNode CommandNode = new CommandNode(ParentNode, CommandToken, Command);
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
				// Match Prefix Option
				BNode PrefixOptionNode = TokenContext.ParsePatternAfter(ParentNode, CommandNode, PrefixOptionPatternFunc._PatternName, BTokenContext._Optional);
				if(PrefixOptionNode != null) {
					return CommandNode.AppendPipedNextNode((CommandNode)PrefixOptionNode);
				}
				// Match Command Symbol
				BNode PipedNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunc._PatternName, BTokenContext._Required);
				if(PipedNode.IsErrorNode()) {
					return PipedNode;
				}
				return CommandNode.AppendPipedNextNode((CommandNode)PipedNode);
			}
			// Match Redirect
			BNode RedirectNode = TokenContext.ParsePattern(ParentNode, RedirectPatternFunc._PatternName, BTokenContext._Optional);
			if(RedirectNode != null) {
				CommandNode.AppendPipedNextNode((CommandNode)RedirectNode);
				continue;
			}
			// Match Suffix Option
			BNode SuffixOptionNode = TokenContext.ParsePattern(ParentNode, SuffixOptionPatternFunc._PatternName, BTokenContext._Optional);
			if(SuffixOptionNode != null) {
				if(SuffixOptionNode.IsErrorNode()) {
					return SuffixOptionNode;
				}
				return CommandNode.AppendPipedNextNode((CommandNode)SuffixOptionNode);
			}
			// Match Argument
			BNode ArgNode = TokenContext.ParsePattern(ParentNode, CommandArgPatternFunc._PatternName, BTokenContext._Optional);
			if(ArgNode == null) {
				break;
			}
			CommandNode.AppendArgNode(ArgNode);
		}
		return CommandNode;
	}
}

class CommandArgPatternFunc extends BMatchFunction {
	public final static String _PatternName = "$CommandArg$";

	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		if(ShellGrammar.matchStopToken(TokenContext)) {
			return null;
		}
		boolean FoundSubstitution = false;
		boolean FoundEscape = false;
		BArray<BToken> TokenList = new BArray<BToken>(new BToken[]{});
		BArray<BNode> NodeList = new BArray<BNode>(new BNode[]{});
		while(!ShellGrammar.matchStopToken(TokenContext)) {
			BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
			if(Token instanceof BPatternToken && ((BPatternToken)Token).PresetPattern.PatternName.equals(DShellStringLiteralPatternFunc.PatternName)) {
				this.Flush(TokenContext, NodeList, TokenList);
				BNode Node = DShellStringLiteralPatternFunc.Interpolate(ParentNode, TokenContext, Token);
				if(Node == null) {
					Node = new BunStringNode(ParentNode, null, LibBunSystem._UnquoteString(Token.GetText()));
				}
				NodeList.add(Node);
			}
			else if(!FoundEscape && Token.EqualsText("$") && !Token.IsNextWhiteSpace() && TokenContext.MatchToken("{")) {
				this.Flush(TokenContext, NodeList, TokenList);
				BNode Node = TokenContext.ParsePattern(ParentNode, "$Expression$", BTokenContext._Required);
				Node = TokenContext.MatchToken(Node, "}", BTokenContext._Required);
				if(Node.IsErrorNode()) {
					return Node;
				}
				Token = TokenContext.LatestToken;
				NodeList.add(Node);
			}
			else if(!FoundEscape && Token.EqualsText("$") && !Token.IsNextWhiteSpace() && TokenContext.GetToken().IsNameSymbol()) {
				this.Flush(TokenContext, NodeList, TokenList);
				Token = TokenContext.GetToken();
				BNode Node = TokenContext.ParsePattern(ParentNode, "$SymbolExpression$", BTokenContext._Required);
				if(Node.IsErrorNode()) {
					return Node;
				}
				NodeList.add(Node);
			}
//			else if(!FoundEscape && Token.EqualsText("`")) {	//TODO
//				
//			}
			else if(!FoundEscape && Token.EqualsText("$") && !Token.IsNextWhiteSpace() && TokenContext.MatchToken("(")) {
				this.Flush(TokenContext, NodeList, TokenList);
				BNode Node = TokenContext.ParsePattern(ParentNode, PrefixOptionPatternFunc._PatternName, BTokenContext._Optional);
				if(Node == null) {
					Node = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunc._PatternName, BTokenContext._Required);
				}
				Node = TokenContext.MatchToken(Node, ")", BTokenContext._Required);
				if(Node instanceof CommandNode) {
					((CommandNode)Node).SetType(BType.StringType);
				}
				Token = TokenContext.LatestToken;
				NodeList.add(Node);
				FoundSubstitution = true;
			}
			else {
				TokenList.add(Token);
			}
			if(Token.IsNextWhiteSpace()) {
				break;
			}
			FoundEscape = this.CheckEscape(Token, FoundEscape);
		}
		this.Flush(TokenContext, NodeList, TokenList);
		BNode ArgNode = new ArgumentNode(ParentNode, FoundSubstitution ? ArgumentNode._Substitution : ArgumentNode._Normal);
		ArgNode.SetNode(ArgumentNode._Expr, ShellGrammar._ToNode(ParentNode, TokenContext, NodeList));
		return ArgNode;
	}

	private boolean CheckEscape(BToken Token, boolean FoundEscape) {
		if(Token.EqualsText("\\") && !FoundEscape) {
			return true;
		}
		return false;
	}

	private void Flush(BTokenContext TokenContext, BArray<BNode> NodeList, BArray<BToken> TokenList) {
		@Var int size = TokenList.size();
		if(size == 0) {
			return;
		}
		int StartIndex = 0;
		int EndIndex = 0;
		for(int i = 0; i < size; i++) {
			if(i == 0) {
				StartIndex = BArray.GetIndex(TokenList, i).StartIndex;
			}
			if(i == size - 1) {
				EndIndex = BArray.GetIndex(TokenList, i).EndIndex;
			}
		}
		BToken Token = new BToken(TokenContext.SourceContext.Source, StartIndex, EndIndex);
		NodeList.add(new BunStringNode(null, Token, LibBunSystem._UnquoteString(Utils.resolveHome(Token.GetText()))));
		TokenList.clear(0);
	}
}

class RedirectPatternFunc extends BMatchFunction {
	public final static String _PatternName = "$Redirect$";

	// <, >, >>, >&, 1>, 2>, 1>>, 2>>, &>, &>>
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
		String RedirectSymbol = Token.GetText();
		if(Token.EqualsText(">>") || Token.EqualsText("<")) {
			return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("&")) {
			BToken Token2 = TokenContext.GetToken(BTokenContext._MoveNext);
			if(Token2.EqualsText(">") || Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
		}
		else if(Token.EqualsText(">")) {
			BToken Token2 = TokenContext.GetToken();
			if(Token2.EqualsText("&")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
			return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("1") || Token.EqualsText("2")) {
			BToken Token2 = TokenContext.GetToken(BTokenContext._MoveNext);
			if(Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
			else if(Token2.EqualsText(">")) {
				RedirectSymbol += Token2.GetText();
				if(RedirectSymbol.equals("2>") && TokenContext.MatchToken("&")) {
					if(TokenContext.MatchToken("1")) {
						return this.CreateRedirectNode(ParentNode, TokenContext, "2>&1", false);
					}
					return null;
				}
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
		}
		return null;
	}

	private BNode CreateRedirectNode(BNode ParentNode, BTokenContext TokenContext, String RedirectSymbol, boolean existTarget) {
		CommandNode Node = new CommandNode(ParentNode, null, RedirectSymbol);
		if(existTarget) {
			BNode TargetNode = TokenContext.ParsePattern(Node, CommandArgPatternFunc._PatternName, BTokenContext._Required);
			if(TargetNode.IsErrorNode()) {
				return TargetNode;
			}
			Node.AppendArgNode(TargetNode);
		}
		return Node;
	}
}

class PrefixOptionPatternFunc extends BMatchFunction {
	public final static String _PatternName = "$PrefixOption$";

	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
		String Symbol = Token.GetText();
		if(Symbol.equals(ShellGrammar.trace)) {
			BNode CommandNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunc._PatternName, BTokenContext._Required);
			if(CommandNode.IsErrorNode()) {
				return CommandNode;
			}
			CommandNode Node = new CommandNode(ParentNode, Token, Symbol);
			return Node.AppendPipedNextNode((CommandNode) CommandNode);
		}
		if(Symbol.equals(ShellGrammar.timeout) && LeftNode == null) {
			BNode TimeNode = this.ParseTimeout(ParentNode, TokenContext);
			if(TimeNode.IsErrorNode()) {
				return TimeNode;
			}
			BNode CommandNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunc._PatternName, BTokenContext._Required);
			if(CommandNode.IsErrorNode()) {
				return CommandNode;
			}
			CommandNode Node = new CommandNode(ParentNode, Token, Symbol);
			Node.AppendArgNode(TimeNode);
			return Node.AppendPipedNextNode((CommandNode) CommandNode);
		}
		return null;
	}

	public BNode ParseTimeout(BNode ParentNode, BTokenContext TokenContext) {
		BToken NumToken = TokenContext.GetToken(BTokenContext._MoveNext);
		if((NumToken instanceof BPatternToken)) {
			if(((BPatternToken)NumToken).PresetPattern.PatternName.equals(("$IntegerLiteral$"))) {
				long Num = LibBunSystem._ParseInt(NumToken.GetText());
				if(Num > 0) {
					if(NumToken.IsNextWhiteSpace()) {
						return new ArgumentNode(ParentNode, Long.toString(Num));
					}
					BToken UnitToken = TokenContext.GetToken(BTokenContext._MoveNext);
					String UnitSymbol = UnitToken.GetText();
					if(UnitSymbol.equals("ms")) {
						return new ArgumentNode(ParentNode, Long.toString(Num));
					}
					if(UnitSymbol.equals("s")) {
						return new ArgumentNode(ParentNode, Long.toString(Num * 1000));
					}
					if(UnitSymbol.equals("m")) {
						return new ArgumentNode(ParentNode, Long.toString(Num * 1000 * 60));
					}
					return TokenContext.CreateExpectedErrorNode(UnitToken, "{ms, s, m}");
				}
			}
		}
		return TokenContext.CreateExpectedErrorNode(NumToken, "Integer Number Symbol");
	}
}

class SuffixOptionPatternFunc extends BMatchFunction {
	public final static String _PatternName = "$SuffixOption$";

	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		String OptionSymbol = Token.GetText();
		if(Token.EqualsText(ShellGrammar.background)) {	// set background job
			return new CommandNode(ParentNode, Token, OptionSymbol);
		}
		return null;
	}
}

class ImportEnvPatternFunc extends BMatchFunction {
	public final static String PatternName = "$ImportEnv$";
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new DShellImportEnvNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "env", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellImportEnvNode._NameInfo, "$Name$", BTokenContext._Required);
		return Node;
	}
}

class ExportEnvPatternFunc extends BMatchFunction {
	public final static String PatternName = "export";
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		TokenContext.MoveNext();
		BNode Node = new DShellExportEnvNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "env", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellExportEnvNode._NameInfo, "$Name$", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, "=", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellExportEnvNode._Expr, "$Expression$", BTokenContext._Required);
		return Node;
	}
}

class DShellBlockPatternFunc extends BMatchFunction {
	public final static String PatternName = "$Block$";

	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode BlockNode = new BunBlockNode(ParentNode, null);
		RuntimeContext.getContext().commandScope.createNewScope();
		//BToken SkipToken = TokenContext.GetToken();
		BlockNode = TokenContext.MatchToken(BlockNode, "{", BTokenContext._Required);
		if(!BlockNode.IsErrorNode()) {
			boolean Remembered = TokenContext.SetParseFlag(BTokenContext._AllowSkipIndent); // init
			while(TokenContext.HasNext()) {
				if(TokenContext.MatchToken("}")) {
					break;
				}
				BlockNode = TokenContext.MatchPattern(BlockNode, BNode._AppendIndex, "$Statement$", BTokenContext._Required);
				if(BlockNode.IsErrorNode()) {
//					TokenContext.SkipError(SkipToken);
					TokenContext.MatchToken("}");
					break;
				}
			}
			TokenContext.SetParseFlag(Remembered);
		}
		RuntimeContext.getContext().commandScope.removeCurrentScope();
		return BlockNode;
	}
}

public class ShellGrammar {
	// suffix option symbol
	public final static String background = "&";
	// prefix option symbol
	public final static String timeout = "timeout";
	public final static String trace = "trace";

	public static boolean matchStopToken(BTokenContext TokenContext) { // ;,)]}&&||
		BToken Token = TokenContext.GetToken();
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

	public static BNode _ToNode(BNode ParentNode, BTokenContext TokenContext, BArray<BNode> NodeList) {
		BNode Node = new BunStringNode(ParentNode, null, "");
		int size = NodeList.size();
		for(int i = 0; i < size; i++) {
			BNode CurrentNode = BArray.GetIndex(NodeList, i);
			BunAddNode BinaryNode = new BunAddNode(ParentNode);
			BinaryNode.SetLeftNode(Node);
			BinaryNode.SetRightNode(CurrentNode);
			Node = BinaryNode;
		}
		return Node;
	}

	public static void LoadGrammar(LibBunGamma Gamma) {
		final BTokenFunction commandSymbolToken = new CommandSymbolTokenFunc();
		final BMatchFunction prefixOptionPattern = new PrefixOptionPatternFunc();

		Gamma.DefineToken("#", new ShellStyleCommentTokenFunc());
		Gamma.DefineToken("Aa_", commandSymbolToken);
		Gamma.DefineToken("1", commandSymbolToken);

		Gamma.DefineStatement("import", new ImportPatternFunc());
		Gamma.DefineExpression(ImportCommandPatternFunc._PatternName, new ImportCommandPatternFunc());
		Gamma.DefineExpression(CommandSymbolPatternFunc._PatternName, new CommandSymbolPatternFunc());
		Gamma.DefineExpression(CommandArgPatternFunc._PatternName, new CommandArgPatternFunc());
		Gamma.DefineExpression(RedirectPatternFunc._PatternName, new RedirectPatternFunc());
		Gamma.DefineExpression(ShellGrammar.timeout, prefixOptionPattern);
		Gamma.DefineExpression(ShellGrammar.trace, prefixOptionPattern);
		Gamma.DefineExpression(PrefixOptionPatternFunc._PatternName, prefixOptionPattern);
		Gamma.DefineExpression(SuffixOptionPatternFunc._PatternName, new SuffixOptionPatternFunc());

		Gamma.DefineStatement(ImportEnvPatternFunc.PatternName, new ImportEnvPatternFunc());
		Gamma.DefineStatement(ExportEnvPatternFunc.PatternName, new ExportEnvPatternFunc());

		// from BultinCommandMap
		ArrayList<String> symbolList = BuiltinCommand.getCommandSymbolList();
		for(String symbol : symbolList) {
			RuntimeContext.getContext().commandScope.setCommand(symbol, symbol);
		}
	}
}
