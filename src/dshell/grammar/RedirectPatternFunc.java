package dshell.grammar;

import dshell.ast.sugar.DShellCommandNode;
import libbun.parser.ast.ZNode;
import libbun.util.Var;
import libbun.util.ZMatchFunction;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class RedirectPatternFunc extends ZMatchFunction {
	public final static String _PatternName = "$Redirect$";

	// <, >, >>, >&, 1>, 2>, 1>>, 2>>, &>, &>>
	@Override public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		@Var String RedirectSymbol = Token.GetText();
		if(Token.EqualsText(">>") || Token.EqualsText("<")) {
			return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("&")) {
			@Var ZToken Token2 = TokenContext.GetToken(ZTokenContext._MoveNext);
			if(Token2.EqualsText(">") || Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
		}
		else if(Token.EqualsText(">")) {
			@Var ZToken Token2 = TokenContext.GetToken();
			if(Token2.EqualsText("&")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
			return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("1") || Token.EqualsText("2")) {
			@Var ZToken Token2 = TokenContext.GetToken(ZTokenContext._MoveNext);
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

	private ZNode CreateRedirectNode(ZNode ParentNode, ZTokenContext TokenContext, String RedirectSymbol, boolean existTarget) {
		@Var DShellCommandNode Node = new DShellCommandNode(ParentNode, null, RedirectSymbol);
		if(existTarget) {
			@Var ZNode TargetNode = TokenContext.ParsePattern(Node, CommandArgPatternFunc._PatternName, ZTokenContext._Required);
			if(TargetNode.IsErrorNode()) {
				return TargetNode;
			}
			Node.AppendArgNode(TargetNode);
		}
		return Node;
	}
}
