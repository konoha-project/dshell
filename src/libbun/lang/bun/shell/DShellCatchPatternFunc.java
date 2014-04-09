package libbun.lang.bun.shell;

import libbun.ast.BNode;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.ast.DShellCatchNode;

public class DShellCatchPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode CatchNode = new DShellCatchNode(ParentNode);
		CatchNode = TokenContext.MatchToken(CatchNode, "catch", BTokenContext._Required);
		CatchNode = TokenContext.MatchToken(CatchNode, "(", BTokenContext._Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, DShellCatchNode._NameInfo, "$Name$", BTokenContext._Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, DShellCatchNode._TypeInfo, "$TypeAnnotation$", BTokenContext._Required);
		CatchNode = TokenContext.MatchToken(CatchNode, ")", BTokenContext._Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, DShellCatchNode._Block, "$Block$", BTokenContext._Required);
		return CatchNode;
	}

}
