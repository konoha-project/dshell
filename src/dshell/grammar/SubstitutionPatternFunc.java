package dshell.grammar;

import dshell.ast.sugar.DShellCommandNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;
import libbun.type.ZType;
import libbun.util.Var;
import libbun.util.ZMatchFunction;

public class SubstitutionPatternFunc extends ZMatchFunction {
	public final static String _PatternName = "$Substitution$";

	@Override public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		if(Token.EqualsText("$") && TokenContext.MatchToken("(")) {
			@Var ZNode Node = TokenContext.ParsePattern(ParentNode, PrefixOptionPatternFunc._PatternName, ZTokenContext._Optional);
			if(Node == null) {
				Node = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunc._PatternName, ZTokenContext._Required);
			}
			Node = TokenContext.MatchToken(Node, ")", ZTokenContext._Required);
			if(Node instanceof DShellCommandNode) {
				((DShellCommandNode)Node).SetType(ZType.StringType);
			}
			return Node;
		}
		else if(Token.EqualsText("`")) {
			//TODO:
		}
		return null;
	}
}
