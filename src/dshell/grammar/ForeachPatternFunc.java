package dshell.grammar;

import libbun.ast.BNode;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.ast.sugar.DShellForeachNode;

public class ForeachPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new DShellForeachNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "for", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, "(", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForeachNode._Value, "$Name$", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, "in", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForeachNode._Expr, "$Expression$", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ")", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForeachNode._Block, "$Block$", BTokenContext._Required);
		return Node;
	}
}
