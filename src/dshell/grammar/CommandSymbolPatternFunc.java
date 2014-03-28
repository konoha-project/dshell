package dshell.grammar;

import dshell.ast.sugar.DShellCommandNode;
import dshell.lang.DShellGrammar;
import libbun.parser.ast.ZErrorNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.util.ZMatchFunction;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class CommandSymbolPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$CommandSymbol$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken CommandToken = TokenContext.GetToken(ZTokenContext._MoveNext);
		ZNode SymbolNode = ParentNode.GetNameSpace().GetSymbol(DShellGrammar.toCommandSymbol(CommandToken.GetText()));
		if(SymbolNode == null || !(SymbolNode instanceof ZStringNode)) {
			return new ZErrorNode(ParentNode, CommandToken, "undefined command symbol");
		}
		String Command = ((ZStringNode)SymbolNode).StringValue;
		DShellCommandNode CommandNode = new DShellCommandNode(ParentNode, CommandToken, Command);
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
				// Match Prefix Option
				ZNode PrefixOptionNode = TokenContext.ParsePatternAfter(ParentNode, CommandNode, PrefixOptionPatternFunc.PatternName, ZTokenContext._Optional);
				if(PrefixOptionNode != null) {
					return CommandNode.AppendPipedNextNode((DShellCommandNode)PrefixOptionNode);
				}
				// Match Command Symbol
				ZNode PipedNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunc.PatternName, ZTokenContext._Required);
				if(PipedNode.IsErrorNode()) {
					return PipedNode;
				}
				return CommandNode.AppendPipedNextNode((DShellCommandNode)PipedNode);
			}
			// Match Redirect
			ZNode RedirectNode = TokenContext.ParsePattern(ParentNode, RedirectPatternFunc.PatternName, ZTokenContext._Optional);
			if(RedirectNode != null) {
				CommandNode.AppendPipedNextNode((DShellCommandNode)RedirectNode);
				continue;
			}
			// Match Suffix Option
			ZNode SuffixOptionNode = TokenContext.ParsePattern(ParentNode, SuffixOptionPatternFunc.PatternName, ZTokenContext._Optional);
			if(SuffixOptionNode != null) {
				if(SuffixOptionNode.IsErrorNode()) {
					return SuffixOptionNode;
				}
				return CommandNode.AppendPipedNextNode((DShellCommandNode)SuffixOptionNode);
			}
			// Match Argument
			ZNode ArgNode = TokenContext.ParsePattern(ParentNode, CommandArgPatternFunc.PatternName, ZTokenContext._Optional);
			if(ArgNode == null) {
				break;
			}
			CommandNode.AppendArgNode(ArgNode);
		}
		return CommandNode;
	}
}
