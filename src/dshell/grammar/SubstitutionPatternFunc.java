package dshell.grammar;

import libbun.parser.ast.ZNode;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;
import libbun.type.ZType;
import libbun.util.Var;
import libbun.util.ZMatchFunction;
import libbun.lang.bun.shell.CommandNode;
import libbun.lang.bun.shell.CommandSymbolPatternFunction;
import libbun.lang.bun.shell.PrefixOptionPatternFunction;

public class SubstitutionPatternFunc extends ZMatchFunction {
	public final static String _PatternName = "$Substitution$";

	@Override public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		if(Token.EqualsText("$") && TokenContext.MatchToken("(")) {
			@Var ZNode Node = TokenContext.ParsePattern(ParentNode, PrefixOptionPatternFunction._PatternName, ZTokenContext._Optional);
			if(Node == null) {
				Node = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunction._PatternName, ZTokenContext._Required);
			}
			Node = TokenContext.MatchToken(Node, ")", ZTokenContext._Required);
			if(Node instanceof CommandNode) {
				((CommandNode)Node).SetType(ZType.StringType);
			}
			return Node;
		}
		else if(Token.EqualsText("`")) {
			//TODO:
		}
		return null;
	}
}
