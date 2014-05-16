package dshell.internal.grammar;

import libbun.ast.BNode;
import libbun.parser.classic.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.internal.ast.DShellForNode;

public class ForPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BNode node = new DShellForNode(parentNode);
		node = tokenContext.MatchToken(node, "for", BTokenContext._Required);
		node = tokenContext.MatchToken(node, "(", BTokenContext._Required);
		node = tokenContext.MatchPattern(node, DShellForNode._Init, "var", BTokenContext._Optional);
		node = tokenContext.MatchToken(node, ";", BTokenContext._Required);
		node = tokenContext.MatchPattern(node, DShellForNode._Cond, "$Expression$", BTokenContext._Required);
		node = tokenContext.MatchToken(node, ";", BTokenContext._Required);
		node = tokenContext.MatchPattern(node, DShellForNode._Next, "$Expression$", BTokenContext._Optional);
		node = tokenContext.MatchToken(node, ")", BTokenContext._Required);
		node = tokenContext.MatchPattern(node, DShellForNode._Block, "$Block$", BTokenContext._Required);
		return node;
	}
}
