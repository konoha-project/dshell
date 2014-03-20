package dshell.lang;

import java.util.ArrayList;

import zen.ast.ZBinaryNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.parser.ZPatternToken;
import zen.parser.ZSource;
import zen.parser.ZSyntax;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class InterpolableStringLiteralToken extends ZPatternToken {
	private ArrayList<ZNode> NodeList;

	public InterpolableStringLiteralToken(ZSource Source, int StartIndex, int EndIndex, ZSyntax PresetPattern) {
		super(Source, StartIndex, EndIndex, PresetPattern);
	}

	public void SetNodeList(ArrayList<ZNode> NodeList) {
		this.NodeList = NodeList;
	}

	public ZNode ToNode(ZNode ParentNode, ZTokenContext TokenContext) {
		ZToken Token = TokenContext.GetToken();
		ZNode Node = new ZStringNode(ParentNode, null, "");
		ZToken PlusToken = new ZToken(new ZSource(Token.GetFileName(), Token.GetLineNumber(), "+", TokenContext), 0, "+".length());
		for(ZNode CurrentNode : this.NodeList) {
			ZBinaryNode BinaryNode = new ZBinaryNode(ParentNode, PlusToken, Node, null);
			BinaryNode.SetNode(ZBinaryNode._Right, CurrentNode);
			Node = BinaryNode;
		}
		return Node;
	}
}
