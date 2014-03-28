package dshell.grammar;

import dshell.ast.sugar.DShellForeachNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ZTokenContext;
import libbun.util.ZMatchFunction;

public class ForeachPatternFunc extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode Node = new DShellForeachNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "for", ZTokenContext._Required);
		Node = TokenContext.MatchToken(Node, "(", ZTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForeachNode._Value, "$Name$", ZTokenContext._Required);
		Node = TokenContext.MatchToken(Node, "in", ZTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForeachNode._Expr, "$Expression$", ZTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ")", ZTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForeachNode._Block, "$Block$", ZTokenContext._Required);
		return Node;
	}
}
