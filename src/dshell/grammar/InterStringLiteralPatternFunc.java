package dshell.grammar;

import java.util.ArrayList;

import dshell.lang.InterStringLiteralToken;
import libbun.parser.ast.ZBinaryNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.parser.ZSource;
import libbun.parser.ZSyntax;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;
import libbun.util.ZMatchFunction;

public class InterStringLiteralPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$InterStringLiteral$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		return ToNode(ParentNode, TokenContext, ((InterStringLiteralToken)Token).GetNodeList());
	}

	public static ZNode ToNode(ZNode ParentNode, ZTokenContext TokenContext, ArrayList<ZNode> NodeList) {
		ZToken Token = TokenContext.GetToken();
		ZNode Node = new ZStringNode(ParentNode, null, "");
		ZSyntax Pattern = TokenContext.NameSpace.GetRightSyntaxPattern("+");
		ZToken PlusToken = new ZToken(new ZSource(Token.GetFileName(), Token.GetLineNumber(), "+", TokenContext), 0, "+".length());
		for(ZNode CurrentNode : NodeList) {
			ZBinaryNode BinaryNode = new ZBinaryNode(ParentNode, PlusToken, Node, Pattern);
			BinaryNode.SetNode(ZBinaryNode._Right, CurrentNode);
			Node = BinaryNode;
		}
		return Node;
	}
}
