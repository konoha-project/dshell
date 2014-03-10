package dshell.grammar;

import dshell.ast.sugar.DShellExportEnvNode;
import zen.ast.ZNode;
import zen.parser.ZTokenContext;
import zen.util.ZMatchFunction;

public class ExportEnvPattern extends ZMatchFunction {
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
