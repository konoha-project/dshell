package dshell.internal.grammar;

import dshell.internal.ast.MatchRegexNode;
import libbun.ast.BNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.parser.classic.BToken;
import libbun.parser.classic.BTokenContext;
import libbun.util.BMatchFunction;

public class MatchRegexPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BToken token = tokenContext.GetToken();
		boolean isUnmatch = token.EqualsText("!~");
		BinaryOperatorNode binaryNode = new MatchRegexNode(parentNode, isUnmatch);
		return binaryNode.SetParsedNode(parentNode, leftNode, binaryNode.GetOperator(), tokenContext);
	}
}
