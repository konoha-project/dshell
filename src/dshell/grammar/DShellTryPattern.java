package dshell.grammar;

import dshell.ast.DShellTryNode;
import zen.ast.ZNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZTokenContext;

public class DShellTryPattern extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode TryNode = new DShellTryNode(ParentNode);
		TryNode = TokenContext.MatchToken(TryNode, "try", ZTokenContext.Required);
		TryNode = TokenContext.MatchPattern(TryNode, DShellTryNode._Try, "$Block$", ZTokenContext.Required);
		int count = 0;
		while(true) {
			if(TokenContext.IsNewLineToken("catch")) {
				TryNode = TokenContext.MatchPattern(TryNode, ZNode.AppendIndex, "$Catch$", ZTokenContext.Required);
				count = count + 1;
				continue;
			}
			if(TokenContext.MatchNewLineToken("finally")) {
				TryNode = TokenContext.MatchPattern(TryNode, DShellTryNode._Finally, "$Block$", ZTokenContext.Required);
				count = count + 1;
			}
			break;
		}
		if(count == 0 && !TryNode.IsErrorNode()) {
			return ((DShellTryNode)TryNode).AST[DShellTryNode._Try]; // no catch and finally
		}
		return TryNode;
	}

}
