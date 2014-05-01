package dshell.grammar;

import dshell.grammar.CommandPatternFunc;
import dshell.grammar.PrefixOptionPatternFunc;
import libbun.ast.BNode;
import dshell.ast.CommandNode;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;

public class LocationPatternFunc extends BMatchFunction {	//TODO
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		CommandNode Node = new CommandNode(ParentNode, Token, DShellGrammar.location);
		Node.AppendArgNode(ParentNode.GetGamma().GetSymbol(Token.GetText()));
		// Match Prefix Option
		BNode PrefixOptionNode = TokenContext.ParsePatternAfter(ParentNode, Node, PrefixOptionPatternFunc._PatternName, BTokenContext._Optional);
		if(PrefixOptionNode != null) {
			return Node.AppendPipedNextNode((CommandNode) PrefixOptionNode);
		}
		// Match Command Symbol
		BNode PipedNode = TokenContext.ParsePattern(ParentNode, CommandPatternFunc._PatternName, BTokenContext._Required);
		if(!PipedNode.IsErrorNode()) {
			return Node.AppendPipedNextNode((CommandNode) PipedNode);
		}
		return null;
	}
}
