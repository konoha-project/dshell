package dshell.grammar;

import java.util.ArrayList;

import dshell.lang.InterpolableStringLiteralToken;
import zen.ast.ZBinaryNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.parser.ZSource;
import zen.parser.ZSyntax;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;
import zen.util.ZMatchFunction;

public class InterpolableStringLiteralPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$InterpolableStringLiteral$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		return ToNode(ParentNode, TokenContext, ((InterpolableStringLiteralToken)Token).GetNodeList());
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
