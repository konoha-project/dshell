package dshell.lang;

import libbun.ast.BNode;
import libbun.parser.BPatternToken;
import libbun.parser.BSource;
import libbun.parser.BSyntax;
import libbun.util.BArray;
import dshell.grammar.DShellStringLiteralPatternFunc;
import dshell.lib.Utils;

public class DShellStringLiteralToken extends BPatternToken {
	private BArray<BNode> NodeList;

	public DShellStringLiteralToken(BSource Source, int StartIndex, int EndIndex) {
		super(Source, StartIndex, EndIndex, null);
		BSyntax Pattern = Source.TokenContext.NameSpace.GetSyntaxPattern(DShellStringLiteralPatternFunc.PatternName);
		if(Pattern == null) {
			Utils.fatal(1, "Pattern is Null");
		}
		this.PresetPattern = Pattern;
	}

	public void SetNodeList(BArray<BNode> NodeList) {
		this.NodeList = NodeList;
	}

	public BArray<BNode> GetNodeList() {
		return this.NodeList;
	}
}
