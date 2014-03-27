package dshell.grammar;

import dshell.ast.sugar.DShellCommandNode;
import zen.ast.ZNode;
import zen.util.ZMatchFunction;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class RedirectPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$Redirect$";
	@Override	// <, >, >>, >&, 1>, 2>, 1>>, 2>>, &>, &>>
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		String RedirectSymbol = Token.GetText();
		if(Token.EqualsText(">>") || Token.EqualsText("<")) {
			return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("&")) {
			ZToken Token2 = TokenContext.GetToken(ZTokenContext._MoveNext);
			if(Token2.EqualsText(">") || Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
		}
		else if(Token.EqualsText(">")) {
			ZToken Token2 = TokenContext.GetToken();
			if(Token2.EqualsText("&")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
			return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("1") || Token.EqualsText("2")) {
			ZToken Token2 = TokenContext.GetToken(ZTokenContext._MoveNext);
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
		DShellCommandNode Node = new DShellCommandNode(ParentNode, null, RedirectSymbol);
		if(existTarget) {
			ZNode TargetNode = TokenContext.ParsePattern(Node, CommandArgPatternFunc.PatternName, ZTokenContext._Required);
			if(TargetNode.IsErrorNode()) {
				return TargetNode;
			}
			Node.AppendArgNode(TargetNode);
		}
		return Node;
	}
}
