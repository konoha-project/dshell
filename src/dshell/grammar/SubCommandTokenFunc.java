package dshell.grammar;

import dshell.lang.SubCommandToken;
import dshell.lib.Utils;
import zen.ast.ZBlockNode;
import zen.ast.ZNode;
import zen.parser.ZSourceContext;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;
import zen.type.ZType;
import zen.util.ZTokenFunction;

public class SubCommandTokenFunc extends ZTokenFunction {
	@Override
	public boolean Invoke(ZSourceContext SourceContext) {
		int StartIndex = SourceContext.GetPosition();
		char KeyChar = SourceContext.GetCurrentChar();
		if(KeyChar == '`') {
			SourceContext.MoveNext();
			return this.Tokenize(SourceContext, StartIndex, '`');
		}
		else if(KeyChar == '$') {
			SourceContext.MoveNext();
			if(SourceContext.GetCurrentChar() == '(') {
				SourceContext.MoveNext();
				return this.Tokenize(SourceContext, StartIndex, ')');
			}
		}
		return false;
	}

	private ZNode MatchCommand(ZSourceContext SourceContext) {
		ZTokenContext TokenContext = SourceContext.TokenContext;
		int RollBackPos = (Integer) Utils.getValue(TokenContext, "CurrentPosition");
		int PrevSize = TokenContext.TokenList.size();
		String Pattern = CommandSymbolPatternFunc.PatternName;
		ZNode Node = TokenContext.ParsePattern(new ZBlockNode(null, TokenContext.NameSpace), Pattern, ZTokenContext._Required);
		Utils.setValue(TokenContext, "CurrentPosition", RollBackPos);
		TokenContext.TokenList.clear(PrevSize);
		Node.Type = ZType.StringType;
		return Node;
	}

	private boolean Tokenize(ZSourceContext SourceContext, int StartIndex, char EndChar) {
		ZNode Node = this.MatchCommand(SourceContext);
		char ch = SourceContext.GetCharAt(SourceContext.GetPosition() - 1);
		if(Node.IsErrorNode() || ch != EndChar) {
			return false;
		}
		int EndIndex = SourceContext.GetPosition();
		SourceContext.Tokenize("$StringLiteral$", StartIndex, EndIndex);
		ZTokenContext TokenContext = SourceContext.TokenContext;
		int size = TokenContext.TokenList.size();
		ZToken Token = new SubCommandToken(SourceContext, StartIndex, EndIndex);
		((SubCommandToken)Token).SetNode(Node);
		TokenContext.TokenList.clear(size - 1);
		TokenContext.TokenList.add(Token);
		return true;
	}
}
