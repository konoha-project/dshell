package dshell.lang;

import dshell.grammar.SubstitutionPatternFunc;
import dshell.lib.Utils;
import zen.ast.ZNode;
import zen.parser.ZPatternToken;
import zen.parser.ZSource;
import zen.parser.ZSyntax;

public class SubstitutionToken extends ZPatternToken {
	private ZNode Node;

	public SubstitutionToken(ZSource Source, int StartIndex, int EndIndex) {
		super(Source, StartIndex, EndIndex, null);
		ZSyntax Pattern = Source.TokenContext.NameSpace.GetSyntaxPattern(SubstitutionPatternFunc.PatternName);
		if(Pattern == null) {
			Utils.fatal(1, "Pattern is Null");
		}
		this.PresetPattern = Pattern;
	}

	public void SetNode(ZNode Node) {
		this.Node = Node;
	}

	public ZNode GetNode() {
		return this.Node;
	}
}
