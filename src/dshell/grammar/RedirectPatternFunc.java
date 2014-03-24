package dshell.grammar;

import dshell.ast.DShellCommandNode;
import dshell.ast.sugar.DShellArgNode;
import zen.ast.ZNode;
import zen.util.ZMatchFunction;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class RedirectPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$Redirect$";
	@Override	// <, >, >>, >&, 1>, 2>, 1>>, 2>>, &>, &>>
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		String RedirectSymbol = Token.GetText();
		if(Token.EqualsText(">>") || Token.EqualsText("<")) {
			return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("&")) {
			ZToken Token2 = TokenContext.GetToken();
			TokenContext.MoveNext();
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
			ZToken Token2 = TokenContext.GetToken();
			TokenContext.MoveNext();
			if(Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
			else if(Token2.EqualsText(">")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
		}
		return null;
	}

	private ZNode CreateRedirectNode(ZNode ParentNode, ZTokenContext TokenContext, String RedirectSymbol, boolean existTarget) {
		ZNode Node = new DShellCommandNode(ParentNode, null);
		Node.SetNode(ZNode._AppendIndex, new DShellArgNode(ParentNode, RedirectSymbol));
		if(existTarget) {
			Node = TokenContext.MatchPattern(Node, ZNode._AppendIndex, CommandArgPatternFunc.PatternName, ZTokenContext._Required);
		}
		return Node;
	}
}
