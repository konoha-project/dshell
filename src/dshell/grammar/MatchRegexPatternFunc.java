package dshell.grammar;

import dshell.ast.MatchRegexNode;
import libbun.ast.BNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;

public class MatchRegexPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BToken Token = TokenContext.GetToken();
		boolean IsUnmatch = Token.EqualsText("!~");
		BinaryOperatorNode BinaryNode = new MatchRegexNode(ParentNode, IsUnmatch);
		return BinaryNode.SetParsedNode(ParentNode, LeftNode, BinaryNode.GetOperator(), TokenContext);
	}
}
