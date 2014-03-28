package dshell.grammar;

import dshell.ast.sugar.DShellAssertNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ZTokenContext;
import libbun.util.ZMatchFunction;

public class AssertPatternFunc extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode Node = new DShellAssertNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "assert", ZTokenContext._Required);
		Node = TokenContext.MatchToken(Node, "(", ZTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellAssertNode._Expr, "$Expression$", ZTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ")", ZTokenContext._Required);
		return Node;
	}
}
