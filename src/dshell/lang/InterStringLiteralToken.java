package dshell.lang;

import java.util.ArrayList;

import dshell.grammar.InterStringLiteralPatternFunc;
import dshell.lib.Utils;
import zen.ast.ZNode;
import zen.parser.ZPatternToken;
import zen.parser.ZSource;
import zen.parser.ZSyntax;

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
