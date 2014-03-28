package dshell.grammar;

import dshell.ast.sugar.DShellExportEnvNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ZTokenContext;
import libbun.util.ZMatchFunction;

public class ExportEnvPatternFunc extends ZMatchFunction {
	public final static String PatternName = "export";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		TokenContext.MoveNext();
		ZNode Node = new DShellExportEnvNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "env", ZTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellExportEnvNode._NameInfo, "$Name$", ZTokenContext._Required);
		Node = TokenContext.MatchToken(Node, "=", ZTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellExportEnvNode._EXPR, "$Expression$", ZTokenContext._Required);
		return Node;
	}
}
