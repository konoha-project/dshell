package dshell.grammar;

import dshell.ast.sugar.DShellCommandNode;
import dshell.lang.DShellGrammar;
import libbun.parser.ast.ZErrorNode;
import libbun.parser.ast.ZNode;
import libbun.util.ZMatchFunction;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class SuffixOptionPatternFunc extends ZMatchFunction {
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
		DShellCommandNode Node = new DShellCommandNode(ParentNode, null, OptionSymbol);
		ZNode PipedNode = TokenContext.ParsePattern(ParentNode, SuffixOptionPatternFunc.PatternName, ZTokenContext._Optional);
		if(PipedNode != null) {
			Node.AppendPipedNextNode((DShellCommandNode)PipedNode);
		}
		if(!DShellGrammar.MatchStopToken(TokenContext)) {
			return new ZErrorNode(ParentNode, TokenContext.GetToken(), "not match stop token");
		}
		return Node;
	}
}
