package libbun.lang.bun.shell;

import libbun.ast.BNode;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.ast.sugar.DShellAssertNode;

public class AssertPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new DShellAssertNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "assert", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, "(", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellAssertNode._Expr, "$Expression$", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ")", BTokenContext._Required);
		return Node;
	}
}
