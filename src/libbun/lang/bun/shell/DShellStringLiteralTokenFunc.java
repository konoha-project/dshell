package libbun.lang.bun.shell;

import libbun.ast.BunBlockNode;
import libbun.ast.BNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.BSourceContext;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.util.BArray;
import libbun.util.BTokenFunction;
import libbun.util.LibBunSystem;
import dshell.lang.DShellStringLiteralToken;
import dshell.lib.Utils;

public class DShellStringLiteralTokenFunc extends BTokenFunction {
	@Override
	public boolean Invoke(BSourceContext SourceContext) {
		
		
//		BArray<BNode> NodeList = new BArray<BNode>(new BNode[]{});
//		boolean FoundExpr = false;
//		int StartIndex = SourceContext.GetPosition();
//		int CurrentIndex = StartIndex + 1;
//		SourceContext.MoveNext();
//		while(SourceContext.HasChar()) {
//			char ch = SourceContext.GetCurrentChar();
//			if(ch == '\n') {
//				break;
//			}
//			else if(ch == '\\') {
//				SourceContext.MoveNext();
//			}
//			else if(ch == '"') {
//				SourceContext.MoveNext(); // eat '"'
//				int EndIndex = SourceContext.GetPosition();
//				SourceContext.Tokenize("$StringLiteral$", StartIndex, EndIndex);
//				if(FoundExpr) {
//					this.CreateAndAppendStringNode(NodeList, SourceContext, CurrentIndex, EndIndex - 1);
//					this.OverrideToken(NodeList, SourceContext, StartIndex, EndIndex);
//				}
//				return true;
//			}
//			else if(ch == '$' && SourceContext.GetCharAtFromCurrentPosition(1) == '{') {
//				this.CreateAndAppendStringNode(NodeList, SourceContext, CurrentIndex, SourceContext.GetPosition());
//				BNode Node = this.CreateExprNode(SourceContext);
//				if(Node != null) {
//					FoundExpr = true;
//					NodeList.add(Node);
//					CurrentIndex = SourceContext.GetPosition();
//					continue;
//				}
//				SourceContext.LogWarning(StartIndex, "not match Expression");
//				break;
//			}
//			//else if((ch == '$' && SourceContext.GetCharAtFromCurrentPosition(1) == '(') || ch == '`') {
//			else if(ch == '$' && SourceContext.GetCharAtFromCurrentPosition(1) == '(') {
//				BTokenContext TokenContext = SourceContext.TokenContext;
//				int RollBackPos = (Integer) Utils.getValue(TokenContext, "CurrentPosition");
//				int PrevSize = TokenContext.TokenList.size();
//				BNode Node = TokenContext.ParsePattern(new BunBlockNode(null, TokenContext.Gamma), SubstitutionPatternFunc._PatternName, BTokenContext._Required);
//				Utils.setValue(TokenContext, "CurrentPosition", RollBackPos);
//				if(!Node.IsErrorNode()) {
//					TokenContext.TokenList.clear(PrevSize);
//					FoundExpr = true;
//					NodeList.add(Node);
//					CurrentIndex = SourceContext.GetPosition();
//					continue;
//				}
//				SourceContext.LogWarning(StartIndex, "not match Command Symbol");
//				break;
//			}
//			SourceContext.MoveNext();
//		}
//		SourceContext.LogWarning(StartIndex, "unclosed \"");
		return false;
	}

	private void CreateAndAppendStringNode(BArray<BNode> NodeList, BSourceContext SourceContext, int StartIndex, int EndIndex) {
		if(StartIndex == EndIndex) {
			return;
		}
		BToken Token = new BToken(SourceContext, StartIndex, EndIndex);
		NodeList.add(new BunStringNode(null, Token, LibBunSystem._UnquoteString(Token.GetText())));
	}

	private BNode CreateExprNode(BSourceContext SourceContext) {
//		SourceContext.MoveNext();
//		SourceContext.MoveNext();
//		BTokenContext TokenContext = SourceContext.TokenContext;
//		int RollBackPos = (Integer) Utils.getValue(TokenContext, "CurrentPosition");
//		int PrevSize = TokenContext.TokenList.size();
//		BNode Node = TokenContext.ParsePattern(new BunBlockNode(null, TokenContext.Gamma), "$Expression$", BTokenContext._Required);
//		char ch = SourceContext.GetCharAt(SourceContext.GetPosition() - 1);
//		Utils.setValue(TokenContext, "CurrentPosition", RollBackPos);
//		if(!Node.IsErrorNode() && ch == '}') {
//			TokenContext.TokenList.clear(PrevSize);
//			return Node;
//		}
		return null;
	}

	private void OverrideToken(BArray<BNode> NodeList, BSourceContext SourceContext, int StartIndex, int EndIndex) {
//		BTokenContext TokenContext = SourceContext.TokenContext;
//		int size = TokenContext.TokenList.size();
//		BToken Token = new DShellStringLiteralToken(SourceContext, StartIndex, EndIndex);
//		((DShellStringLiteralToken)Token).SetNodeList(NodeList);
//		TokenContext.TokenList.clear(size - 1);
//		TokenContext.TokenList.add(Token);
	}
}
