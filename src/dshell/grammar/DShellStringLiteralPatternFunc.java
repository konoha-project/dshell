package dshell.grammar;

import dshell.lang.DShellStringLiteralToken;
import libbun.lang.bun.shell.ShellUtils;
import libbun.parser.ast.ZNode;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;
import libbun.util.ZMatchFunction;

public class DShellStringLiteralPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$InterStringLiteral$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		return ShellUtils._ToNode(ParentNode, TokenContext, ((DShellStringLiteralToken)Token).GetNodeList());
	}
}
