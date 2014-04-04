package dshell.grammar;

import libbun.ast.BNode;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.ast.sugar.DShellExportEnvNode;

public class ExportEnvPatternFunc extends BMatchFunction {
	public final static String PatternName = "export";
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		TokenContext.MoveNext();
		BNode Node = new DShellExportEnvNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "env", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellExportEnvNode._NameInfo, "$Name$", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, "=", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellExportEnvNode._Expr, "$Expression$", BTokenContext._Required);
		return Node;
	}
}
