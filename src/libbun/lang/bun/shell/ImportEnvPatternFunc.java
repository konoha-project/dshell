package libbun.lang.bun.shell;

import libbun.ast.BNode;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.ast.sugar.DShellImportEnvNode;

public class ImportEnvPatternFunc extends BMatchFunction {
	public final static String PatternName = "$ImportEnv$";
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new DShellImportEnvNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "env", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellImportEnvNode._NameInfo, "$Name$", BTokenContext._Required);
		return Node;
	}
}
