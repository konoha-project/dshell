package dshell.lang;

import dshell.grammar.DShellStringLiteralPatternFunc;
import dshell.lib.Utils;
import libbun.parser.ast.ZNode;
import libbun.parser.ZPatternToken;
import libbun.parser.ZSource;
import libbun.parser.ZSyntax;
import libbun.util.ZArray;

public class DShellStringLiteralToken extends ZPatternToken {
	private ZArray<ZNode> NodeList;

	public DShellStringLiteralToken(ZSource Source, int StartIndex, int EndIndex) {
		super(Source, StartIndex, EndIndex, null);
		ZSyntax Pattern = Source.TokenContext.NameSpace.GetSyntaxPattern(DShellStringLiteralPatternFunc.PatternName);
		if(Pattern == null) {
			Utils.fatal(1, "Pattern is Null");
		}
		this.PresetPattern = Pattern;
	}

	public void SetNodeList(ZArray<ZNode> NodeList) {
		this.NodeList = NodeList;
	}

	public ZArray<ZNode> GetNodeList() {
		return this.NodeList;
	}
}
