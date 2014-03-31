package dshell.grammar;

import dshell.ast.sugar.DShellCommandNode;
import dshell.lang.DShellGrammar;
import libbun.parser.ast.ZErrorNode;
import libbun.parser.ast.ZNode;
import libbun.util.Var;
import libbun.util.ZMatchFunction;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class SuffixOptionPatternFunc extends ZMatchFunction {
	public final static String _PatternName = "$SuffixOption$";

	@Override public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		@Var String OptionSymbol = Token.GetText();
		if(Token.EqualsText(DShellGrammar.background)) {	// set background job
			return this.CreateNodeAndMatchNextOption(ParentNode, TokenContext, OptionSymbol);
		}
		return null;
	}

	public ZNode CreateNodeAndMatchNextOption(ZNode ParentNode, ZTokenContext TokenContext, String OptionSymbol) {
		@Var DShellCommandNode Node = new DShellCommandNode(ParentNode, null, OptionSymbol);
		@Var ZNode PipedNode = TokenContext.ParsePattern(ParentNode, SuffixOptionPatternFunc._PatternName, ZTokenContext._Optional);
		if(PipedNode != null) {
			Node.AppendPipedNextNode((DShellCommandNode)PipedNode);
		}
		if(!DShellGrammar.MatchStopToken(TokenContext)) {
			return new ZErrorNode(ParentNode, TokenContext.GetToken(), "not match stop token");
		}
		return Node;
	}
}
