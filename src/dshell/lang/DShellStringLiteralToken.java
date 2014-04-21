package dshell.lang;

import libbun.ast.BNode;
import libbun.lang.bun.shell.DShellStringLiteralPatternFunc;
import libbun.parser.BPatternToken;
import libbun.parser.LibBunSource;
import libbun.parser.LibBunSyntax;
import libbun.util.BArray;
import dshell.lib.Utils;

public class DShellStringLiteralToken extends BPatternToken {
	private BArray<BNode> NodeList;

	public DShellStringLiteralToken(LibBunSource Source, int StartIndex, int EndIndex) {
		super(Source, StartIndex, EndIndex, null);
		//LibBunSyntax Pattern = Source.TokenContext.Gamma.GetSyntaxPattern(DShellStringLiteralPatternFunc.PatternName);
		LibBunSyntax Pattern = null;
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
