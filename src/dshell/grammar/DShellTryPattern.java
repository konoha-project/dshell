package dshell.grammar;

import dshell.ast.DShellTryNode;
import zen.ast.ZNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZTokenContext;

public class DShellTryPattern extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode TryNode = new DShellTryNode(ParentNode);
		TryNode = TokenContext.MatchToken(TryNode, "try", ZTokenContext._Required);
		TryNode = TokenContext.MatchPattern(TryNode, DShellTryNode._Try, "$Block$", ZTokenContext._Required);
		int count = 0;
		while(true) {
			if(TokenContext.IsNewLineToken("catch")) {
				TryNode = TokenContext.MatchPattern(TryNode, ZNode._AppendIndex, "$Catch$", ZTokenContext._Required);
				count = count + 1;
				continue;
			}
			if(TokenContext.MatchNewLineToken("finally")) {
				TryNode = TokenContext.MatchPattern(TryNode, DShellTryNode._Finally, "$Block$", ZTokenContext._Required);
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
