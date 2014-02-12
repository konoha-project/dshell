package dshell.grammar;

import zen.ast.ZCatchNode;
import zen.ast.ZNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZTokenContext;

public class DShellCatchPattern extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode CatchNode = new ZCatchNode(ParentNode);
		CatchNode = TokenContext.MatchToken(CatchNode, "catch", ZTokenContext.Required);
		CatchNode = TokenContext.MatchToken(CatchNode, "(", ZTokenContext.Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, ZNode.NameInfo, "$Name$", ZTokenContext.Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, ZNode.TypeInfo, "$TypeAnnotation$", ZTokenContext.Required);
		CatchNode = TokenContext.MatchToken(CatchNode, ")", ZTokenContext.Required);
		CatchNode = TokenContext.MatchPattern(CatchNode, ZCatchNode._Block, "$Block$", ZTokenContext.Required);
		return CatchNode;
	}

}
