package dshell.grammar;

import java.util.ArrayList;

import dshell.lang.DShellGrammar;
import dshell.lang.InterpolableStringLiteralToken;
import dshell.lib.Utils;
import zen.ast.ZBinaryNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.util.LibZen;
import zen.util.ZMatchFunction;
import zen.parser.ZNameSpace;
import zen.parser.ZSource;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class CommandArgPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$CommandArg$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		if(DShellGrammar.MatchStopToken(TokenContext)) {
			return null;
		}
		ArgumentBuilder argBuilder = new ArgumentBuilder(ParentNode, TokenContext);
		ZToken Token;
		do {
			Token = TokenContext.GetToken();
			if(Token instanceof InterpolableStringLiteralToken) {
				argBuilder.appendAsNode(Token);
			}
			else {
				if(!argBuilder.foundEscapeSequence() && Token.EqualsText("$")) {
					Token = this.MatchStringInterpolation(TokenContext);
					if(Token == null) {
						return null;
					}
					argBuilder.append(Token.GetText(), true);
				}
				else {
					argBuilder.append(Token.GetText(), false);
				}
			}
			Token = TokenContext.GetToken(ZTokenContext._MoveNext);
			if(Token.IsNextWhiteSpace()) {
				break;
			}
		} while(!DShellGrammar.MatchStopToken(TokenContext));
		return argBuilder.buildArgNode();
	}
	
	private ZToken MatchStringInterpolation(ZTokenContext TokenContext) {
		ZToken KeyToken = TokenContext.GetToken(ZTokenContext._MoveNext);
		String Symbol = KeyToken.GetText();
		ZToken Token = TokenContext.GetToken();
		int braceCount = 0;
		if(Token.EqualsText("{")) {
			while(TokenContext.HasNext()) {
				ZToken BodyToken = TokenContext.GetToken();
				if(BodyToken.EqualsText("{")) {
					braceCount++;
				}
				if(BodyToken.EqualsText("}") && --braceCount == 0) {
					Symbol += BodyToken.GetText();
					ZSource Source = new ZSource(KeyToken.GetFileName(), KeyToken.GetLineNumber(), Symbol, TokenContext);
					return new ZToken(Source, 0, Symbol.length());
				}
				Symbol += BodyToken.GetText();
				if(BodyToken.IsNextWhiteSpace()) {
					Symbol += " ";
				}
				TokenContext.MoveNext();
			}
		}
		else if(Token.IsNameSymbol()) {
			Symbol += "{" + Token.GetText() + "}";
			ZSource Source = new ZSource(KeyToken.GetFileName(), KeyToken.GetLineNumber(), Symbol, TokenContext);
			return new ZToken(Source, 0, Symbol.length());
		}
		return null;
	}

	private static class ArgumentBuilder {
		private class Element {
			public final String tokenText;
			public final boolean isExpr;
			public Element(String tokenText, boolean isExpr) {
				this.isExpr = isExpr;
				this.tokenText = this.isExpr ? tokenText.substring(2, tokenText.length() - 1) : tokenText;
			}
			public ZNode toNode() {
				if(this.isExpr) {
					ZNameSpace NameSpace = ParentNode.GetNameSpace();
					ZTokenContext LocalContext = new ZTokenContext(TokenContext.Generator, NameSpace, fileName, lineNum, this.tokenText);
					return LocalContext.ParsePattern(ParentNode, "$Expression$", ZTokenContext._Required);
				}
				else {
					ZSource source = new ZSource(fileName, lineNum, this.tokenText, TokenContext);
					ZToken token = new ZToken(source, 0, this.tokenText.length());
					return new ZStringNode(ParentNode, token, LibZen._UnquoteString(this.tokenText));
				}
			}
		}

		private class NodeElement extends Element {
			private final ZNode node;
			public NodeElement(ZNode node) {
				super("", false);
				this.node = node;
			}
			@Override
			public ZNode toNode() {
				return this.node;
			}
		}

		private final ZNode ParentNode;
		private final ZTokenContext TokenContext;
		private final String fileName;
		private final int lineNum;
		private ArrayList<Element> elementList;
		private StringBuilder tokenTextBuffer;
		private boolean foundError = false;
		private boolean foundEscapseSequence = false;

		public ArgumentBuilder(ZNode ParentNode, ZTokenContext TokenContext) {
			this.ParentNode = ParentNode;
			this.TokenContext = TokenContext;
			ZToken token = TokenContext.GetToken();
			this.fileName = token.GetFileName();
			this.lineNum = token.GetLineNumber();
			this.elementList = new ArrayList<Element>();
			this.tokenTextBuffer = null;
		}

		public void appendAsNode(ZToken token) {
			ZNode node = ((InterpolableStringLiteralToken)token).ToNode(this.ParentNode, this.TokenContext);
			this.elementList.add(new NodeElement(node));
		}

		public void append(String tokenText, boolean isExpr) {
			if(!this.foundEscapseSequence && tokenText.equals("\\")) {
				this.foundEscapseSequence = true;
			}
			else {
				this.foundEscapseSequence = false;
			}
			if(isExpr) {
				this.flushBuffer();
				this.elementList.add(new Element(tokenText, true));
			}
			else {
				if(this.tokenTextBuffer == null) {
					this.tokenTextBuffer = new StringBuilder();
				}
				if(tokenText.equals("~")) {
					tokenText = Utils.getEnv("HOME");
				}
				this.tokenTextBuffer.append(tokenText);
			}
		}

		private void flushBuffer() {
			if(this.tokenTextBuffer != null) {
				this.elementList.add(new Element(this.tokenTextBuffer.toString(), false));
				this.tokenTextBuffer = null;
			}
		}

		public boolean foundEscapeSequence() {
			return this.foundEscapseSequence;
		}

		public ZNode buildArgNode() {
			this.flushBuffer();
			if(this.foundError) {
				return null;
			}
			ZToken plusToken = new ZToken(new ZSource(fileName, lineNum, "+", TokenContext), 0, "+".length());
			ZToken token = new ZToken(new ZSource(fileName, lineNum, "", TokenContext), 0, "".length());
			ZNode node = new ZStringNode(ParentNode, token, "");
			for(Element pair : this.elementList) {
				ZBinaryNode binaryNode = new ZBinaryNode(ParentNode, plusToken, node, null);
				binaryNode.SetNode(ZBinaryNode._Right, pair.toNode());
				node = binaryNode;
			}
			return node;
		}
	}
}
