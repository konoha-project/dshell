package dshell.grammar;

import libbun.ast.BNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.parser.LibBunSource;
import libbun.util.BArray;
import libbun.util.BMatchFunction;
import libbun.util.LibBunSystem;

public class DShellStringLiteralPatternFunc extends BMatchFunction {
	public final static String PatternName = "$InterStringLiteral$";
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
		BNode Node  = Interpolate(ParentNode, TokenContext, Token);
		if(Node != null) {
			return Node;
		}
		return new BunStringNode(ParentNode, Token, LibBunSystem._UnquoteString(Token.GetText()));
	}

	public static BNode Interpolate(BNode ParentNode, BTokenContext TokenContext, BToken Token) {
		BArray<BNode> NodeList = new BArray<BNode>(new BNode[]{});
		int StartIndex = Token.StartIndex + 1;
		final int EndIndex = Token.EndIndex - 1;
		int CurrentIndex = StartIndex;
		LibBunSource Source = TokenContext.SourceContext.Source;
		while(CurrentIndex < EndIndex) {
			char ch = Source.GetCharAt(CurrentIndex);
			if(ch == '\\') {
				CurrentIndex++;
			}
			else if(ch == '$') {
				char next = Source.GetCharAt(CurrentIndex + 1);
				if(next == '(' || next == '{') {
					CreateStringNode(NodeList, ParentNode, TokenContext, StartIndex, CurrentIndex);
					StartIndex = CurrentIndex = CreateExprNode(NodeList, ParentNode, TokenContext, CurrentIndex, EndIndex);
					if(CurrentIndex == -1) {
						return null;
					}
					continue;
				}
			}
			CurrentIndex++;
		}
		CreateStringNode(NodeList, ParentNode, TokenContext, StartIndex, CurrentIndex);
		return ShellGrammar._ToNode(ParentNode, TokenContext, NodeList);
	}

	private static int CreateExprNode(BArray<BNode> NodeList, BNode ParentNode, BTokenContext TokenContext, int CurrentIndex, int EndIndex) {
		char ch = TokenContext.SourceContext.Source.GetCharAt(CurrentIndex + 1);
		if(ch == '{') {
			BTokenContext LocalContext = TokenContext.SubContext(CurrentIndex + 2, EndIndex);
			BNode Node = LocalContext.ParsePattern(ParentNode, "$Expression$", BTokenContext._Required);
			BToken CloseToken = LocalContext.GetToken();
			if(!Node.IsErrorNode() && CloseToken.EqualsText("}")) {
				NodeList.add(Node);
				return CloseToken.EndIndex;
			}
		}
		else {
			BTokenContext LocalContext = TokenContext.SubContext(CurrentIndex, EndIndex);
			BNode Node = LocalContext.ParsePattern(ParentNode, SubstitutionPatternFunc._PatternName, BTokenContext._Required);
			if(!Node.IsErrorNode()) {
				NodeList.add(Node);
				return LocalContext.LatestToken.EndIndex;
			}
		}
		return -1;
	}

	private static void CreateStringNode(BArray<BNode> NodeList, BNode ParentNode, BTokenContext TokenContext, int StartIndex, int CurrentIndex) {
		if(StartIndex == CurrentIndex) {
			return;
		}
		BToken Token = new BToken(TokenContext.SourceContext.Source, StartIndex, CurrentIndex);
		BNode Node = new BunStringNode(ParentNode, null, LibBunSystem._UnquoteString(Token.GetText()));
		NodeList.add(Node);
	}
}
