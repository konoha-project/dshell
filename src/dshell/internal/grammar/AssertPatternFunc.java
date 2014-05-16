package dshell.internal.grammar;

import libbun.ast.BNode;
import libbun.parser.classic.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.internal.ast.sugar.DShellAssertNode;

public class AssertPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BNode node = new DShellAssertNode(parentNode);
		node = tokenContext.MatchToken(node, "assert", BTokenContext._Required);
		node = tokenContext.MatchToken(node, "(", BTokenContext._Required);
		node = tokenContext.MatchPattern(node, DShellAssertNode._Expr, "$Expression$", BTokenContext._Required);
		node = tokenContext.MatchToken(node, ")", BTokenContext._Required);
		return node;
	}
}
