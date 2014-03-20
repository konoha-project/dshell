package dshell.lang;

import java.util.ArrayList;

import zen.ast.ZNode;
import zen.parser.ZPatternToken;
import zen.parser.ZSource;
import zen.parser.ZSyntax;

public class InterpolableStringLiteralToken extends ZPatternToken {
	private ArrayList<ZNode> NodeList;

	public InterpolableStringLiteralToken(ZSource Source, int StartIndex, int EndIndex, ZSyntax PresetPattern) {
		super(Source, StartIndex, EndIndex, PresetPattern);
	}

	public void SetNodeList(ArrayList<ZNode> NodeList) {
		this.NodeList = NodeList;
	}

	public ArrayList<ZNode> GetNodeList() {
		return this.NodeList;
	}
}
