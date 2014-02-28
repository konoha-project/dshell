package dshell.grammar;

import dshell.ast.DShellCommandNode;
import dshell.lang.DShellGrammar;
import zen.ast.ZErrorNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class SuffixOptionPattern extends ZMatchFunction {
	public final static String PatternName = "$SuffixOption$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		String OptionSymbol = Token.GetText();
		if(Token.EqualsText(DShellGrammar.background)) {	// set background job
			return this.CreateNodeAndMatchNextOption(ParentNode, TokenContext, OptionSymbol);
		}
		return null;
	}

	public ZNode CreateNodeAndMatchNextOption(ZNode ParentNode, ZTokenContext TokenContext, String OptionSymbol) {
		ZNode Node = new DShellCommandNode(ParentNode, null);
		Node.Set(ZNode._AppendIndex, new ZStringNode(ParentNode, null, OptionSymbol));
		ZNode PipedNode = TokenContext.ParsePattern(ParentNode, SuffixOptionPattern.PatternName, ZTokenContext._Optional);
		if(PipedNode != null) {
			((DShellCommandNode)Node).AppendPipedNextNode((DShellCommandNode)PipedNode);
		}
		if(!DShellGrammar.MatchStopToken(TokenContext)) {
			return new ZErrorNode(ParentNode, TokenContext.GetToken(), "not match stop token");
		}
		return Node;
	}
}
