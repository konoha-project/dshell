package dshell.grammar;

import dshell.ast.DShellCatchNode;
import zen.ast.ZCatchNode;
import zen.ast.ZNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZTokenContext;

public class DShellCatchPattern extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode CatchNode = new DShellCatchNode(ParentNode);
		CatchNode = TokenContext.MatchToken(CatchNode, "catch", ZTokenContext._Required);
		CatchNode = TokenContext.MatchToken(CatchNode, "(", ZTokenContext._Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, ZNode._NameInfo, "$Name$", ZTokenContext._Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, ZNode._TypeInfo, "$TypeAnnotation$", ZTokenContext._Required);
		CatchNode = TokenContext.MatchToken(CatchNode, ")", ZTokenContext._Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, ZCatchNode._Block, "$Block$", ZTokenContext._Required);
		return CatchNode;
	}

}
