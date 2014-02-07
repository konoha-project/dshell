package dshell.grammar;

import dshell.ast.DShellCommandNode;
import dshell.lang.DShellGrammar;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class LocationPattern extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		ZStringNode KeyNode = new ZStringNode(ParentNode, null, DShellGrammar.location);
		DShellCommandNode Node = new DShellCommandNode(ParentNode, Token);
		Node.Set(ZNode.AppendIndex, KeyNode);
		Node.Append(ParentNode.GetNameSpace().GetSymbolNode(Token.GetText()));
		ZNode PipedNode = TokenContext.ParsePattern(ParentNode, "$DShell$", ZTokenContext.Required);
		if(!PipedNode.IsErrorNode()) {
			return Node.AppendPipedNextNode((DShellCommandNode) PipedNode);
		}
		return null;
	}

}
