package dshell.grammar;

import dshell.lang.SubstitutionToken;
import zen.ast.ZNode;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;
import zen.util.ZMatchFunction;

public class SubstitutionPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$Substitution$";

	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		return ((SubstitutionToken)Token).GetNode();
	}
}
