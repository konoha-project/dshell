package dshell.internal.grammar;

import libbun.ast.BNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.classic.BToken;
import libbun.parser.classic.BTokenContext;
import libbun.parser.classic.ParserSource;
import libbun.util.BArray;
import libbun.util.BMatchFunction;
import libbun.util.LibBunSystem;

public class DoubleQuoteStringLiteralPatternFunc extends BMatchFunction {
	public final static String patternName = "$InterStringLiteral$";
	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BToken token = tokenContext.GetToken(BTokenContext._MoveNext);
		BNode node  = interpolate(parentNode, tokenContext, token);
		if(node != null) {
			return node;
		}
		return new BunStringNode(parentNode, token, LibBunSystem._UnquoteString(token.GetText()));
	}

	public static BNode interpolate(BNode parentNode, BTokenContext tokenContext, BToken token) {
		BArray<BNode> nodeList = new BArray<BNode>(new BNode[]{});
		boolean foundExpr = false;
		int startIndex = token.StartIndex + 1;
		final int endIndex = token.EndIndex - 1;
		int currentIndex = startIndex;
		ParserSource source = tokenContext.SourceContext.Source;
		while(currentIndex < endIndex) {
			char ch = source.GetCharAt(currentIndex);
			if(ch == '\\') {
				currentIndex++;
			}
			else if(ch == '$') {
				char next = source.GetCharAt(currentIndex + 1);
				if(next == '(' || next == '{') {
					foundExpr = true;
					createStringNode(nodeList, parentNode, tokenContext, startIndex, currentIndex);
					startIndex = currentIndex = createExprNode(nodeList, parentNode, tokenContext, currentIndex, endIndex);
					if(currentIndex == -1) {
						return null;
					}
					continue;
				}
			}
			currentIndex++;
		}
		if(!foundExpr) {
			return null;
		}
		createStringNode(nodeList, parentNode, tokenContext, startIndex, currentIndex);
		return ShellGrammar.toNode(parentNode, tokenContext, nodeList);
	}

	private static int createExprNode(BArray<BNode> nodeList, BNode parentNode, BTokenContext tokenContext, int currentIndex, int endIndex) {
		char ch = tokenContext.SourceContext.Source.GetCharAt(currentIndex + 1);
		if(ch == '{') {
			BTokenContext localContext = tokenContext.SubContext(currentIndex + 2, endIndex);
			BNode node = localContext.ParsePattern(parentNode, "$Expression$", BTokenContext._Required);
			BToken closeToken = localContext.GetToken();
			if(!node.IsErrorNode() && closeToken.EqualsText("}")) {
				nodeList.add(node);
				return closeToken.EndIndex;
			}
		}
		else {
			BTokenContext localContext = tokenContext.SubContext(currentIndex, endIndex);
			BNode node = localContext.ParsePattern(parentNode, SubstitutionPatternFunc._PatternName, BTokenContext._Required);
			if(!node.IsErrorNode()) {
				nodeList.add(node);
				return localContext.LatestToken.EndIndex;
			}
		}
		return -1;
	}

	private static void createStringNode(BArray<BNode> nodeList, BNode parentNode, BTokenContext tokenContext, int startIndex, int currentIndex) {
		if(startIndex == currentIndex) {
			return;
		}
		BToken token = new BToken(tokenContext.SourceContext.Source, startIndex, currentIndex);
		BNode node = new BunStringNode(parentNode, null, LibBunSystem._UnquoteString(token.GetText()));
		nodeList.add(node);
	}
}
