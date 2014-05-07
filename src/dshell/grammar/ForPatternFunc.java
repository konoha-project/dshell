package dshell.grammar;

import libbun.ast.BNode;
import libbun.parser.classic.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.ast.DShellForNode;

public class ForPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new DShellForNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "for", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, "(", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForNode._Init, "var", BTokenContext._Optional);
		Node = TokenContext.MatchToken(Node, ";", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForNode._Cond, "$Expression$", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ";", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForNode._Next, "$Expression$", BTokenContext._Optional);
		Node = TokenContext.MatchToken(Node, ")", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForNode._Block, "$Block$", BTokenContext._Required);
		return Node;
	}
}
