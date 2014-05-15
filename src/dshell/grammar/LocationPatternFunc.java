package dshell.grammar;

import dshell.grammar.CommandPatternFunc;
import dshell.grammar.PrefixOptionPatternFunc;
import libbun.ast.BNode;
import dshell.ast.CommandNode;
import libbun.parser.classic.BToken;
import libbun.parser.classic.BTokenContext;
import libbun.util.BMatchFunction;

public class LocationPatternFunc extends BMatchFunction {	//TODO
	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BToken token = tokenContext.GetToken();
		tokenContext.MoveNext();
		CommandNode node = new CommandNode(parentNode, token, DShellGrammar.location);
		node.appendArgNode(parentNode.GetGamma().GetSymbol(token.GetText()));
		// Match Prefix Option
		BNode prefixOptionNode = tokenContext.ParsePatternAfter(parentNode, node, PrefixOptionPatternFunc.patternName, BTokenContext._Optional);
		if(prefixOptionNode != null) {
			return node.appendPipedNextNode((CommandNode) prefixOptionNode);
		}
		// Match Command Symbol
		BNode pipedNode = tokenContext.ParsePattern(parentNode, CommandPatternFunc.patternName, BTokenContext._Required);
		if(!pipedNode.IsErrorNode()) {
			return node.appendPipedNextNode((CommandNode) pipedNode);
		}
		return null;
	}
}
