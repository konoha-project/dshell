package dshell.grammar;

import dshell.ast.DShellForNode;
import zen.ast.ZNode;
import zen.parser.ZTokenContext;
import zen.util.ZMatchFunction;

public class ForPatternFunc extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode Node = new DShellForNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "for", ZTokenContext._Required);
		Node = TokenContext.MatchToken(Node, "(", ZTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForNode._Init, "var", ZTokenContext._Optional);
		Node = TokenContext.MatchToken(Node, ";", ZTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForNode._Cond, "$Expression$", ZTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ";", ZTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForNode._Next, "$SymbolStatement$", ZTokenContext._Optional);
		Node = TokenContext.MatchToken(Node, ")", ZTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellForNode._Block, "$Block$", ZTokenContext._Required);
		return Node;
	}

}
