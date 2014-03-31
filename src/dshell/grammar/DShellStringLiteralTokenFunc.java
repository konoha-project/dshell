package dshell.grammar;

import dshell.lang.DShellStringLiteralToken;
import dshell.lib.Utils;

import libbun.parser.ast.ZBlockNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.parser.ZSourceContext;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;
import libbun.util.LibZen;
import libbun.util.ZArray;
import libbun.util.ZTokenFunction;

public class DShellStringLiteralTokenFunc extends ZTokenFunction{
	@Override
	public boolean Invoke(ZSourceContext SourceContext) {
		ZArray<ZNode> NodeList = new ZArray<ZNode>(new ZNode[]{});
		boolean FoundExpr = false;
		int StartIndex = SourceContext.GetPosition();
		int CurrentIndex = StartIndex + 1;
		SourceContext.MoveNext();
		while(SourceContext.HasChar()) {
			char ch = SourceContext.GetCurrentChar();
			if(ch == '\n') {
				break;
			}
			else if(ch == '\\') {
				SourceContext.MoveNext();
			}
			else if(ch == '"') {
				SourceContext.MoveNext(); // eat '"'
				int EndIndex = SourceContext.GetPosition();
				SourceContext.Tokenize("$StringLiteral$", StartIndex, EndIndex);
				if(FoundExpr) {
					this.CreateAndAppendStringNode(NodeList, SourceContext, CurrentIndex, EndIndex - 1);
					this.OverrideToken(NodeList, SourceContext, StartIndex, EndIndex);
				}
				return true;
			}
			else if(ch == '$' && SourceContext.GetCharAtFromCurrentPosition(1) == '{') {
				this.CreateAndAppendStringNode(NodeList, SourceContext, CurrentIndex, SourceContext.GetPosition());
				ZNode Node = this.CreateExprNode(SourceContext);
				if(Node != null) {
					FoundExpr = true;
					NodeList.add(Node);
					CurrentIndex = SourceContext.GetPosition();
					continue;
				}
				SourceContext.LogWarning(StartIndex, "not match Expression");
				break;
			}
			//else if((ch == '$' && SourceContext.GetCharAtFromCurrentPosition(1) == '(') || ch == '`') {
			else if(ch == '$' && SourceContext.GetCharAtFromCurrentPosition(1) == '(') {
				ZTokenContext TokenContext = SourceContext.TokenContext;
				int RollBackPos = (Integer) Utils.getValue(TokenContext, "CurrentPosition");
				int PrevSize = TokenContext.TokenList.size();
				ZNode Node = TokenContext.ParsePattern(new ZBlockNode(null, TokenContext.NameSpace), SubstitutionPatternFunc._PatternName, ZTokenContext._Required);
				Utils.setValue(TokenContext, "CurrentPosition", RollBackPos);
				if(!Node.IsErrorNode()) {
					TokenContext.TokenList.clear(PrevSize);
					FoundExpr = true;
					NodeList.add(Node);
					CurrentIndex = SourceContext.GetPosition();
					continue;
				}
				SourceContext.LogWarning(StartIndex, "not match Command Symbol");
				break;
			}
			SourceContext.MoveNext();
		}
		SourceContext.LogWarning(StartIndex, "unclosed \"");
		return false;
	}

	private void CreateAndAppendStringNode(ZArray<ZNode> NodeList, ZSourceContext SourceContext, int StartIndex, int EndIndex) {
		if(StartIndex == EndIndex) {
			return;
		}
		ZToken Token = new ZToken(SourceContext, StartIndex, EndIndex);
		NodeList.add(new ZStringNode(null, Token, LibZen._UnquoteString(Token.GetText())));
	}

	private ZNode CreateExprNode(ZSourceContext SourceContext) {
		SourceContext.MoveNext();
		SourceContext.MoveNext();
		ZTokenContext TokenContext = SourceContext.TokenContext;
		int RollBackPos = (Integer) Utils.getValue(TokenContext, "CurrentPosition");
		int PrevSize = TokenContext.TokenList.size();
		ZNode Node = TokenContext.ParsePattern(new ZBlockNode(null, TokenContext.NameSpace), "$Expression$", ZTokenContext._Required);
		char ch = SourceContext.GetCharAt(SourceContext.GetPosition() - 1);
		Utils.setValue(TokenContext, "CurrentPosition", RollBackPos);
		if(!Node.IsErrorNode() && ch == '}') {
			TokenContext.TokenList.clear(PrevSize);
			return Node;
		}
		return null;
	}

	private void OverrideToken(ZArray<ZNode> NodeList, ZSourceContext SourceContext, int StartIndex, int EndIndex) {
		ZTokenContext TokenContext = SourceContext.TokenContext;
		int size = TokenContext.TokenList.size();
		ZToken Token = new DShellStringLiteralToken(SourceContext, StartIndex, EndIndex);
		((DShellStringLiteralToken)Token).SetNodeList(NodeList);
		TokenContext.TokenList.clear(size - 1);
		TokenContext.TokenList.add(Token);
	}
}
