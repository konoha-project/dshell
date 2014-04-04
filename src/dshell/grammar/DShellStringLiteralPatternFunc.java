package dshell.grammar;

import dshell.lang.DShellStringLiteralToken;
import libbun.ast.BNode;
import libbun.lang.bun.shell.ShellUtils;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;

public class DShellStringLiteralPatternFunc extends BMatchFunction {
	public final static String PatternName = "$InterStringLiteral$";
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
		return ShellUtils._ToNode(ParentNode, TokenContext, ((DShellStringLiteralToken)Token).GetNodeList());
	}
}
