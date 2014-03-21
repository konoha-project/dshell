package dshell.grammar;

import java.util.ArrayList;

import dshell.lang.InterStringLiteralToken;
import dshell.lib.Utils;

import zen.ast.ZBlockNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.parser.ZSourceContext;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;
import zen.util.LibZen;
import zen.util.ZTokenFunction;

public class InterStringLiteralTokenFunc extends ZTokenFunction{
	@Override
	public boolean Invoke(ZSourceContext SourceContext) {
		ArrayList<ZNode> NodeList = new ArrayList<ZNode>();
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
				FoundExpr = true;
				this.CreateAndAppendStringNode(NodeList, SourceContext, CurrentIndex, SourceContext.GetPosition());
				if(this.CreateAndAppendExprNode(NodeList, SourceContext)) {
					CurrentIndex = SourceContext.GetPosition();
					continue;
				}
				else {
					SourceContext.LogWarning(StartIndex, "not match Expression");
					break;
				}
			}
			SourceContext.MoveNext();
		}
		SourceContext.LogWarning(StartIndex, "unclosed \"");
		return false;
	}

	private void CreateAndAppendStringNode(ArrayList<ZNode> NodeList, ZSourceContext SourceContext, int StartIndex, int EndIndex) {
		if(StartIndex == EndIndex) {
			return;
		}
		ZToken Token = new ZToken(SourceContext, StartIndex, EndIndex);
		NodeList.add(new ZStringNode(null, Token, LibZen._UnquoteString(Token.GetText())));
	}

	private boolean CreateAndAppendExprNode(ArrayList<ZNode> NodeList, ZSourceContext SourceContext) {
		SourceContext.MoveNext();
		SourceContext.MoveNext();
		ZTokenContext TokenContext = SourceContext.TokenContext;
		int RollBackPos = (Integer) Utils.getValue(TokenContext, "CurrentPosition");
		int prevSize = TokenContext.TokenList.size();
		ZNode Node = TokenContext.ParsePattern(new ZBlockNode(null, TokenContext.NameSpace), "$Expression$", ZTokenContext._Required);
		char ch = SourceContext.GetCharAt(SourceContext.GetPosition() - 1);
		Utils.setValue(TokenContext, "CurrentPosition", RollBackPos);
		if(!Node.IsErrorNode() && ch == '}') {
			NodeList.add(Node);
			TokenContext.TokenList.clear(prevSize);
			return true;
		}
		return false;
	}

	private void OverrideToken(ArrayList<ZNode> NodeList, ZSourceContext SourceContext, int StartIndex, int EndIndex) {
		ZTokenContext TokenContext = SourceContext.TokenContext;
		int size = TokenContext.TokenList.size();
		ZToken Token = new InterStringLiteralToken(SourceContext, StartIndex, EndIndex);
		((InterStringLiteralToken)Token).SetNodeList(NodeList);
		TokenContext.TokenList.clear(size - 1);
		TokenContext.TokenList.add(Token);
	}
}
