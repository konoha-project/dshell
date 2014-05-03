package dshell.grammar;

import libbun.ast.BNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;
import libbun.util.LibBunSystem;

public class SingleQuoteStringLiteralPatternFunc extends BMatchFunction {
	public final static String patternName = "$StringLiteral$";

	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BToken token = tokenContext.GetToken(BTokenContext._MoveNext);
		return new BunStringNode(parentNode, token, LibBunSystem._UnquoteString(token.GetText()));
	}
}
