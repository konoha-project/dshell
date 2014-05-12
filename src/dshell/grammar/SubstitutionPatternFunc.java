package dshell.grammar;

import dshell.grammar.PrefixOptionPatternFunc;
import libbun.type.BType;
import libbun.util.BMatchFunction;
import libbun.ast.BNode;
import dshell.ast.CommandNode;
import libbun.parser.classic.BToken;
import libbun.parser.classic.BTokenContext;

public class SubstitutionPatternFunc extends BMatchFunction {
	public final static String _PatternName = "$Substitution$";

	@Override public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BToken token = tokenContext.GetToken(BTokenContext._MoveNext);
		if(token.EqualsText("$") && tokenContext.MatchToken("(")) {
			BNode node = tokenContext.ParsePattern(parentNode, PrefixOptionPatternFunc._PatternName, BTokenContext._Optional);
			if(node == null) {
				node = tokenContext.ParsePattern(parentNode, CommandPatternFunc._PatternName, BTokenContext._Required);
			}
			node = tokenContext.MatchToken(node, ")", BTokenContext._Required);
			if(node instanceof CommandNode) {
				((CommandNode)node).setType(BType.StringType);
			}
			return node;
		}
		else if(token.EqualsText("`")) {
			//TODO:
		}
		return null;
	}
}
