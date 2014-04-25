package dshell.grammar;

import dshell.grammar.PrefixOptionPatternFunc;
import libbun.type.BType;
import libbun.util.BMatchFunction;
import libbun.util.Var;
import libbun.ast.BNode;
import dshell.ast.sugar.CommandNode;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;

public class SubstitutionPatternFunc extends BMatchFunction {
	public final static String _PatternName = "$Substitution$";

	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
		if(Token.EqualsText("$") && TokenContext.MatchToken("(")) {
			@Var BNode Node = TokenContext.ParsePattern(ParentNode, PrefixOptionPatternFunc._PatternName, BTokenContext._Optional);
			if(Node == null) {
				Node = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunc._PatternName, BTokenContext._Required);
			}
			Node = TokenContext.MatchToken(Node, ")", BTokenContext._Required);
			if(Node instanceof CommandNode) {
				((CommandNode)Node).SetType(BType.StringType);
			}
			return Node;
		}
		else if(Token.EqualsText("`")) {
			//TODO:
		}
		return null;
	}
}
