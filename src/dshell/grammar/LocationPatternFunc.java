package dshell.grammar;

import dshell.ast.sugar.DShellCommandNode;
import dshell.lang.DShellGrammar;
import libbun.parser.ast.ZNode;
import libbun.util.ZMatchFunction;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class LocationPatternFunc extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		DShellCommandNode Node = new DShellCommandNode(ParentNode, Token, DShellGrammar.location);
		Node.AppendArgNode(ParentNode.GetNameSpace().GetSymbol(Token.GetText()));
		// Match Prefix Option
		ZNode PrefixOptionNode = TokenContext.ParsePatternAfter(ParentNode, Node, PrefixOptionPatternFunc.PatternName, ZTokenContext._Optional);
		if(PrefixOptionNode != null) {
			return Node.AppendPipedNextNode((DShellCommandNode) PrefixOptionNode);
		}
		// Match Command Symbol
		ZNode PipedNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunc.PatternName, ZTokenContext._Required);
		if(!PipedNode.IsErrorNode()) {
			return Node.AppendPipedNextNode((DShellCommandNode) PipedNode);
		}
		return null;
	}
}
