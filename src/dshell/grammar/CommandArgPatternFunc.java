package dshell.grammar;

import dshell.lang.DShellStringLiteralToken;
import dshell.lib.Utils;
import libbun.ast.BNode;
import libbun.ast.literal.BunStringNode;
import libbun.lang.bun.shell.ArgumentNode;
import libbun.lang.bun.shell.CommandNode;
import libbun.lang.bun.shell.CommandSymbolPatternFunction;
import libbun.lang.bun.shell.PrefixOptionPatternFunction;
import libbun.lang.bun.shell.ShellUtils;
import libbun.parser.BPatternToken;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.type.BType;
import libbun.util.BArray;
import libbun.util.BLib;
import libbun.util.BMatchFunction;
import libbun.util.Var;

public class CommandArgPatternFunc extends BMatchFunction {
	public final static String _PatternName = "$CommandArg$";

	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		if(ShellUtils._MatchStopToken(TokenContext)) {
			return null;
		}
		@Var boolean FoundSubstitution = false;
		@Var boolean FoundEscape = false;
		@Var BArray<BToken> TokenList = new BArray<BToken>(new BToken[]{});
		@Var BArray<BNode> NodeList = new BArray<BNode>(new BNode[]{});
		while(!ShellUtils._MatchStopToken(TokenContext)) {
			@Var BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
			if(Token instanceof DShellStringLiteralToken) {
				this.Flush(TokenContext, NodeList, TokenList);
				@Var DShellStringLiteralToken InterStringToken = (DShellStringLiteralToken) Token;
				NodeList.add(ShellUtils._ToNode(ParentNode, TokenContext, InterStringToken.GetNodeList()));
			}
			else if(Token instanceof BPatternToken && ((BPatternToken)Token).PresetPattern.equals("$StringLiteral$")) {
				this.Flush(TokenContext, NodeList, TokenList);
				NodeList.add(new BunStringNode(ParentNode, null, BLib._UnquoteString(Token.GetText())));
			}
			else if(!FoundEscape && Token.EqualsText("$") && !Token.IsNextWhiteSpace() && TokenContext.MatchToken("{")) {
				this.Flush(TokenContext, NodeList, TokenList);
				@Var BNode Node = TokenContext.ParsePattern(ParentNode, "$Expression$", BTokenContext._Required);
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
				@Var BNode Node = TokenContext.ParsePattern(ParentNode, "$SymbolExpression$", BTokenContext._Required);
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
				@Var BNode Node = TokenContext.ParsePattern(ParentNode, PrefixOptionPatternFunction._PatternName, BTokenContext._Optional);
				if(Node == null) {
					Node = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunction._PatternName, BTokenContext._Required);
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
		@Var BNode ArgNode = new ArgumentNode(ParentNode, FoundSubstitution ? ArgumentNode._Substitution : ArgumentNode._Normal);
		ArgNode.SetNode(ArgumentNode._Expr, ShellUtils._ToNode(ParentNode, TokenContext, NodeList));
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
		@Var int StartIndex = 0;
		@Var int EndIndex = 0;
		for(int i = 0; i < size; i++) {
			if(i == 0) {
				StartIndex = BArray.GetIndex(TokenList, i).StartIndex;
			}
			if(i == size - 1) {
				EndIndex = BArray.GetIndex(TokenList, i).EndIndex;
			}
		}
		@Var BToken Token = new BToken(TokenContext.Source, StartIndex, EndIndex);
		NodeList.add(new BunStringNode(null, Token, BLib._UnquoteString(this.ResolveHome(Token.GetText()))));
		TokenList.clear(0);
	}

	public String ResolveHome(String Path) {
		return Utils.resolveHome(Path);
	}
}
