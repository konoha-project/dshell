package libbun.lang.bun.shell;

import libbun.ast.BNode;
import libbun.ast.literal.BunStringNode;
import libbun.encode.LibBunGenerator;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.parser.LibBunParser;
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
		String TokenText = Token.GetText();
		String SourceText = TokenText.substring(1, TokenText.length() - 1);
		int StartIndex = 0;
		int CurrentIndex = 0;
		int Size = SourceText.length();
		while(CurrentIndex < Size) {
			char ch = SourceText.charAt(CurrentIndex);
			switch(ch) {
			case '\\':
				CurrentIndex++;
				break;
			case '$':{
				char next = SourceText.charAt(CurrentIndex + 1);
				if(next == '(' || next == '{') {
					CreateStringNode(NodeList, SourceText, ParentNode, StartIndex, CurrentIndex);
					StartIndex = CurrentIndex = CreateExprNode(NodeList, ParentNode, TokenContext, SourceText, Token, CurrentIndex);
					if(CurrentIndex == -1) {
						return null;
					}
					continue;
				}
			}
				break;
			}
			CurrentIndex++;
		}
		CreateStringNode(NodeList, SourceText, ParentNode, StartIndex, CurrentIndex);
		return ShellUtils._ToNode(ParentNode, TokenContext, NodeList);
	}

	private static int CreateExprNode(BArray<BNode> NodeList, BNode ParentNode, BTokenContext TokenContext, String SourceText, BToken Token, int CurrentIndex) {
		String FileName = Token.GetFileName();
		int LineNumber = Token.GetLineNumber();
		LibBunParser Parser = TokenContext.Parser;
		LibBunGenerator Generator = TokenContext.Generator;
		char ch = SourceText.charAt(CurrentIndex + 1);
		if(ch == '{') {
			BTokenContext LocalContext = new BTokenContext(Parser, Generator, FileName, LineNumber, SourceText.substring(CurrentIndex + 2));
			BNode Node = LocalContext.ParsePattern(ParentNode, "$Expression$", BTokenContext._Required);
			BToken CloseToken = LocalContext.GetToken();
			if(!Node.IsErrorNode() && CloseToken.EqualsText("}")) {
				NodeList.add(Node);
				return CloseToken.EndIndex + CurrentIndex + 2;
			}
		}
		else {
			BTokenContext LocalContext = new BTokenContext(Parser, Generator, FileName, LineNumber, SourceText.substring(CurrentIndex));
			BNode Node = LocalContext.ParsePattern(ParentNode, SubstitutionPatternFunc._PatternName, BTokenContext._Required);
			if(!Node.IsErrorNode()) {
				NodeList.add(Node);
				return LocalContext.LatestToken.EndIndex + CurrentIndex;
			}
		}
		return -1;
	}

	private static void CreateStringNode(BArray<BNode> NodeList, String SourceText, BNode ParentNode, int StartIndex, int CurrentIndex) {
		if(StartIndex == CurrentIndex) {
			return;
		}
		BNode Node = new BunStringNode(ParentNode, null, LibBunSystem._UnquoteString(SourceText.substring(StartIndex, CurrentIndex)));
		NodeList.add(Node);
	}
}
