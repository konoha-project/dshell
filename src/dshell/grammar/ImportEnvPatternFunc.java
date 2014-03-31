package dshell.grammar;

import dshell.ast.sugar.DShellImportEnvNode;
import libbun.parser.ast.ZNode;
import libbun.util.ZMatchFunction;
import libbun.parser.ZTokenContext;

public class ImportEnvPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$ImportEnv$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode Node = new DShellImportEnvNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "env", ZTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellImportEnvNode._NameInfo, "$Name$", ZTokenContext._Required);
		return Node;
	}
}
