package dshell.grammar;

import dshell.ast.DShellCatchNode;
import zen.ast.ZNode;
import zen.util.ZMatchFunction;
import zen.parser.ZTokenContext;

public class DShellCatchPattern extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode CatchNode = new DShellCatchNode(ParentNode);
		CatchNode = TokenContext.MatchToken(CatchNode, "catch", ZTokenContext._Required);
		CatchNode = TokenContext.MatchToken(CatchNode, "(", ZTokenContext._Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, DShellCatchNode._NameInfo, "$Name$", ZTokenContext._Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, DShellCatchNode._TypeInfo, "$TypeAnnotation$", ZTokenContext._Required);
		CatchNode = TokenContext.MatchToken(CatchNode, ")", ZTokenContext._Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, DShellCatchNode._Block, "$Block$", ZTokenContext._Required);
		return CatchNode;
	}

}
