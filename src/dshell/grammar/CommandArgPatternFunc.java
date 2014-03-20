package dshell.grammar;

import java.util.ArrayList;

import dshell.lang.DShellGrammar;
import dshell.lang.InterpolableStringLiteralToken;
import dshell.lib.Utils;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.util.LibZen;
import zen.util.ZMatchFunction;
import zen.parser.ZPatternToken;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class CommandArgPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$CommandArg$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		if(DShellGrammar.MatchStopToken(TokenContext)) {
			return null;
		}
		boolean FoundEscape = false;
		ArrayList<ZNode> NodeList = new ArrayList<ZNode>();
		StringNodeBuilder NodeBuilder = new StringNodeBuilder(ParentNode);
		while(!DShellGrammar.MatchStopToken(TokenContext)) {
			ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
			if(Token instanceof InterpolableStringLiteralToken) {
				NodeBuilder.Flush(NodeList);
				InterpolableStringLiteralToken InterStringToken = (InterpolableStringLiteralToken) Token;
				NodeList.add(InterpolableStringLiteralPatternFunc.ToNode(ParentNode, TokenContext, InterStringToken.GetNodeList()));
			}
			else if(!FoundEscape && Token.EqualsText("$") && !Token.IsNextWhiteSpace() && TokenContext.MatchToken("{")) {
				NodeBuilder.Flush(NodeList);
				ZNode Node = TokenContext.ParsePattern(ParentNode, "$Expression$", ZTokenContext._Required);
				Token = TokenContext.GetToken(ZTokenContext._MoveNext);
				if(Node.IsErrorNode() || !Token.EqualsText('}')) {
					return null;
				}
				NodeList.add(Node);
			}
			else if(!FoundEscape && Token.EqualsText("$") && !Token.IsNextWhiteSpace() && TokenContext.GetToken().IsNameSymbol()) {
				NodeBuilder.Flush(NodeList);
				ZNode Node = TokenContext.ParsePattern(ParentNode, "$SymbolExpression$", ZTokenContext._Required);
				Token = TokenContext.LatestToken;
				if(Node.IsErrorNode()) {
					return null;
				}
				NodeList.add(Node);
			}
			else {
				NodeBuilder.Append(Token);
			}
			FoundEscape = this.CheckEscape(Token, FoundEscape);
			if(Token.IsNextWhiteSpace()) {
				break;
			}
		}
		NodeBuilder.Flush(NodeList);
		return InterpolableStringLiteralPatternFunc.ToNode(ParentNode, TokenContext, NodeList);
	}

	private boolean CheckEscape(ZToken Token, boolean FoundEscape) {
		if(Token.EqualsText("\\") && !FoundEscape) {
			return true;
		}
		return false;
	}

	private class StringNodeBuilder {
		private final ZNode ParentNode;
		private StringBuilder TokenBuffer;

		public StringNodeBuilder(ZNode ParentNode) {
			this.ParentNode = ParentNode;
			this.TokenBuffer = null;
		}

		public void Append(ZToken Token) {
			if(this.TokenBuffer == null) {
				this.TokenBuffer = new StringBuilder();
			}
			String TokenText = Token.GetText();
			if(Token instanceof ZPatternToken) {
				ZPatternToken PatternToken = (ZPatternToken) Token;
				if(PatternToken.PresetPattern.PatternName.equals("$StringLiteral$")) {
					this.TokenBuffer.append(TokenText.substring(1, TokenText.length() - 1));
					return;
				}
			}
			
			this.TokenBuffer.append(TokenText);
		}

		public void Flush(ArrayList<ZNode> NodeList) {
			if(this.TokenBuffer != null) {
				String Value = Utils.ResolveHome(this.TokenBuffer.toString());
				NodeList.add(new ZStringNode(ParentNode, null, LibZen._UnquoteString(Value)));
				this.TokenBuffer = null;
			}
		}
	}
}
