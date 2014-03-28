package dshell.lang;

import java.util.ArrayList;

import dshell.grammar.InterStringLiteralPatternFunc;
import dshell.lib.Utils;
import libbun.parser.ast.ZNode;
import libbun.parser.ZPatternToken;
import libbun.parser.ZSource;
import libbun.parser.ZSyntax;

public class InterStringLiteralToken extends ZPatternToken {
	private ArrayList<ZNode> NodeList;

	public InterStringLiteralToken(ZSource Source, int StartIndex, int EndIndex) {
		super(Source, StartIndex, EndIndex, null);
		ZSyntax Pattern = Source.TokenContext.NameSpace.GetSyntaxPattern(InterStringLiteralPatternFunc.PatternName);
		if(Pattern == null) {
			Utils.fatal(1, "Pattern is Null");
		}
		this.PresetPattern = Pattern;
	}

	public void SetNodeList(ArrayList<ZNode> NodeList) {
		this.NodeList = NodeList;
	}

	public ArrayList<ZNode> GetNodeList() {
		return this.NodeList;
	}
}
