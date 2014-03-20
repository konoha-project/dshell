package dshell.grammar;

import dshell.lang.InterpolableStringLiteralToken;
import zen.ast.ZNode;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;
import zen.util.ZMatchFunction;

public class InterpolableStringLiteralPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$InterpolableStringLiteral$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		return ((InterpolableStringLiteralToken)Token).ToNode(ParentNode, TokenContext);
	}
}
