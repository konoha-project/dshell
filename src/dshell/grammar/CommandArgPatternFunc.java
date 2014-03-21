package dshell.grammar;

import java.util.ArrayList;

import dshell.lang.DShellGrammar;
import dshell.lang.InterStringLiteralToken;
import dshell.lang.SubCommandToken;
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
			if(Token instanceof InterStringLiteralToken) {
				NodeBuilder.Flush(NodeList);
				InterStringLiteralToken InterStringToken = (InterStringLiteralToken) Token;
				NodeList.add(InterStringLiteralPatternFunc.ToNode(ParentNode, TokenContext, InterStringToken.GetNodeList()));
			}
			else if(Token instanceof SubCommandToken) {
				NodeBuilder.Flush(NodeList);
				SubCommandToken SubCmdToken = (SubCommandToken) Token;
				NodeList.add(SubCmdToken.GetNode());
			}
			else if(!FoundEscape && Token.EqualsText("$") && !Token.IsNextWhiteSpace() && TokenContext.MatchToken("{")) {
				NodeBuilder.Flush(NodeList);
				ZNode Node = TokenContext.ParsePattern(ParentNode, "$Expression$", ZTokenContext._Required);
				Node = TokenContext.MatchToken(Node, "}", ZTokenContext._Required);
				if(Node.IsErrorNode()) {
					return Node;
				}
				Token = TokenContext.LatestToken;
				NodeList.add(Node);
			}
			else if(!FoundEscape && Token.EqualsText("$") && !Token.IsNextWhiteSpace() && TokenContext.GetToken().IsNameSymbol()) {
				NodeBuilder.Flush(NodeList);
				ZNode Node = TokenContext.ParsePattern(ParentNode, "$SymbolExpression$", ZTokenContext._Required);
				if(Node.IsErrorNode()) {
					return Node;
				}
				Token = TokenContext.LatestToken;
				NodeList.add(Node);
			}
			else {
				NodeBuilder.Append(Token);
			}
			if(Token.IsNextWhiteSpace()) {
				break;
			}
			FoundEscape = this.CheckEscape(Token, FoundEscape);
		}
		NodeBuilder.Flush(NodeList);
		return InterStringLiteralPatternFunc.ToNode(ParentNode, TokenContext, NodeList);
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
