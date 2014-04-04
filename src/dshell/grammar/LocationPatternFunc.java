package dshell.grammar;

import dshell.lang.DShellGrammar;
import libbun.ast.BNode;
import libbun.lang.bun.shell.CommandNode;
import libbun.lang.bun.shell.CommandSymbolPatternFunction;
import libbun.lang.bun.shell.PrefixOptionPatternFunction;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;

public class LocationPatternFunc extends BMatchFunction {	//TODO
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		CommandNode Node = new CommandNode(ParentNode, Token, DShellGrammar.location);
		Node.AppendArgNode(ParentNode.GetNameSpace().GetSymbol(Token.GetText()));
		// Match Prefix Option
		BNode PrefixOptionNode = TokenContext.ParsePatternAfter(ParentNode, Node, PrefixOptionPatternFunction._PatternName, BTokenContext._Optional);
		if(PrefixOptionNode != null) {
			return Node.AppendPipedNextNode((CommandNode) PrefixOptionNode);
		}
		// Match Command Symbol
		BNode PipedNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunction._PatternName, BTokenContext._Required);
		if(!PipedNode.IsErrorNode()) {
			return Node.AppendPipedNextNode((CommandNode) PipedNode);
		}
		return null;
	}
}
