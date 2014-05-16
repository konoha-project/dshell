package dshell.internal.grammar;

import libbun.ast.BNode;
import libbun.parser.classic.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.internal.ast.sugar.DShellForeachNode;

public class ForeachPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BNode node = new DShellForeachNode(parentNode);
		node = tokenContext.MatchToken(node, "for", BTokenContext._Required);
		node = tokenContext.MatchToken(node, "(", BTokenContext._Required);
		node = tokenContext.MatchPattern(node, DShellForeachNode._Value, "$Name$", BTokenContext._Required);
		node = tokenContext.MatchToken(node, "in", BTokenContext._Required);
		node = tokenContext.MatchPattern(node, DShellForeachNode._Expr, "$Expression$", BTokenContext._Required);
		node = tokenContext.MatchToken(node, ")", BTokenContext._Required);
		node = tokenContext.MatchPattern(node, DShellForeachNode._Block, "$Block$", BTokenContext._Required);
		return node;
	}
}
