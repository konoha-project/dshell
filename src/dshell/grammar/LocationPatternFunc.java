package dshell.grammar;

import dshell.lang.DShellGrammar;
import libbun.lang.bun.shell.CommandNode;
import libbun.lang.bun.shell.CommandSymbolPatternFunction;
import libbun.lang.bun.shell.PrefixOptionPatternFunction;
import libbun.parser.ast.ZNode;
import libbun.util.ZMatchFunction;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class LocationPatternFunc extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		CommandNode Node = new CommandNode(ParentNode, Token, DShellGrammar.location);
		Node.AppendArgNode(ParentNode.GetNameSpace().GetSymbol(Token.GetText()));
		// Match Prefix Option
		ZNode PrefixOptionNode = TokenContext.ParsePatternAfter(ParentNode, Node, PrefixOptionPatternFunction._PatternName, ZTokenContext._Optional);
		if(PrefixOptionNode != null) {
			return Node.AppendPipedNextNode((CommandNode) PrefixOptionNode);
		}
		// Match Command Symbol
		ZNode PipedNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunction._PatternName, ZTokenContext._Required);
		if(!PipedNode.IsErrorNode()) {
			return Node.AppendPipedNextNode((CommandNode) PipedNode);
		}
		return null;
	}
}
