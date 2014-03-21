package dshell.grammar;

import dshell.lang.SubCommandToken;
import zen.ast.ZNode;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;
import zen.util.ZMatchFunction;

public class SubCommandPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$SubCommand$";

	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		return ((SubCommandToken)Token).GetNode();
	}
}
