package dshell.grammar;

import dshell.ast.sugar.DShellAssertNode;
import zen.ast.ZNode;
import zen.parser.ZTokenContext;
import zen.util.ZMatchFunction;

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
