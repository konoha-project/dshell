package dshell.grammar;

import dshell.lang.DShellStringLiteralToken;
import dshell.lib.Utils;
import libbun.lang.bun.shell.ArgumentNode;
import libbun.lang.bun.shell.CommandNode;
import libbun.lang.bun.shell.CommandSymbolPatternFunction;
import libbun.lang.bun.shell.PrefixOptionPatternFunction;
import libbun.lang.bun.shell.ShellUtils;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.type.ZType;
import libbun.util.LibZen;
import libbun.util.Var;
import libbun.util.ZArray;
import libbun.util.ZMatchFunction;
import libbun.parser.ZPatternToken;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class CommandArgPatternFunc extends ZMatchFunction {
	public final static String _PatternName = "$CommandArg$";

	@Override public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		if(ShellUtils._MatchStopToken(TokenContext)) {
			return null;
		}
		@Var boolean FoundSubstitution = false;
		@Var boolean FoundEscape = false;
		@Var ZArray<ZToken> TokenList = new ZArray<ZToken>(new ZToken[]{});
		@Var ZArray<ZNode> NodeList = new ZArray<ZNode>(new ZNode[]{});
		while(!ShellUtils._MatchStopToken(TokenContext)) {
			@Var ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
			if(Token instanceof DShellStringLiteralToken) {
				this.Flush(TokenContext, NodeList, TokenList);
				@Var DShellStringLiteralToken InterStringToken = (DShellStringLiteralToken) Token;
				NodeList.add(ShellUtils._ToNode(ParentNode, TokenContext, InterStringToken.GetNodeList()));
			}
			else if(Token instanceof ZPatternToken && ((ZPatternToken)Token).PresetPattern.equals("$StringLiteral$")) {
				this.Flush(TokenContext, NodeList, TokenList);
				NodeList.add(new ZStringNode(ParentNode, null, LibZen._UnquoteString(Token.GetText())));
			}
			else if(!FoundEscape && Token.EqualsText("$") && !Token.IsNextWhiteSpace() && TokenContext.MatchToken("{")) {
				this.Flush(TokenContext, NodeList, TokenList);
				@Var ZNode Node = TokenContext.ParsePattern(ParentNode, "$Expression$", ZTokenContext._Required);
				Node = TokenContext.MatchToken(Node, "}", ZTokenContext._Required);
				if(Node.IsErrorNode()) {
					return Node;
				}
				Token = TokenContext.LatestToken;
				NodeList.add(Node);
			}
			else if(!FoundEscape && Token.EqualsText("$") && !Token.IsNextWhiteSpace() && TokenContext.GetToken().IsNameSymbol()) {
				this.Flush(TokenContext, NodeList, TokenList);
				Token = TokenContext.GetToken();
				@Var ZNode Node = TokenContext.ParsePattern(ParentNode, "$SymbolExpression$", ZTokenContext._Required);
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
				@Var ZNode Node = TokenContext.ParsePattern(ParentNode, PrefixOptionPatternFunction._PatternName, ZTokenContext._Optional);
				if(Node == null) {
					Node = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunction._PatternName, ZTokenContext._Required);
				}
				Node = TokenContext.MatchToken(Node, ")", ZTokenContext._Required);
				if(Node instanceof CommandNode) {
					((CommandNode)Node).SetType(ZType.StringType);
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
		@Var ZNode ArgNode = new ArgumentNode(ParentNode, FoundSubstitution ? ArgumentNode._Substitution : ArgumentNode._Normal);
		ArgNode.SetNode(ArgumentNode._Expr, ShellUtils._ToNode(ParentNode, TokenContext, NodeList));
		return ArgNode;
	}

	private boolean CheckEscape(ZToken Token, boolean FoundEscape) {
		if(Token.EqualsText("\\") && !FoundEscape) {
			return true;
		}
		return false;
	}

	private void Flush(ZTokenContext TokenContext, ZArray<ZNode> NodeList, ZArray<ZToken> TokenList) {
		@Var int size = TokenList.size();
		if(size == 0) {
			return;
		}
		@Var int StartIndex = 0;
		@Var int EndIndex = 0;
		for(int i = 0; i < size; i++) {
			if(i == 0) {
				StartIndex = ZArray.GetIndex(TokenList, i).StartIndex;
			}
			if(i == size - 1) {
				EndIndex = ZArray.GetIndex(TokenList, i).EndIndex;
			}
		}
		@Var ZToken Token = new ZToken(TokenContext.Source, StartIndex, EndIndex);
		NodeList.add(new ZStringNode(null, Token, LibZen._UnquoteString(this.ResolveHome(Token.GetText()))));
		TokenList.clear(0);
	}

	public String ResolveHome(String Path) {
		return Utils.resolveHome(Path);
	}
}
