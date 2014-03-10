package dshell.grammar;

import dshell.ast.DShellCommandNode;
import dshell.lang.DShellGrammar;
import zen.ast.ZErrorNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.util.ZMatchFunction;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class CommandSymbolPattern extends ZMatchFunction {
	public final static String PatternName = "$CommandSymbol$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken CommandToken = TokenContext.GetToken(ZTokenContext._MoveNext);
		ZNode SymbolNode = ParentNode.GetNameSpace().GetSymbolNode(DShellGrammar.toCommandSymbol(CommandToken.GetText()));
		if(SymbolNode == null || !(SymbolNode instanceof ZStringNode)) {
			return new ZErrorNode(ParentNode, CommandToken, "undefined command symbol");
		}
		String Command = ((ZStringNode)SymbolNode).StringValue;
		ZNode CommandNode = new DShellCommandNode(ParentNode, CommandToken);
		CommandNode.Set(ZNode._AppendIndex, new ZStringNode(ParentNode, CommandToken, Command));
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
				// Match Prefix Option
				ZNode PrefixOptionNode = TokenContext.ParsePatternAfter(ParentNode, CommandNode, PrefixOptionPattern.PatternName, ZTokenContext._Optional);
				if(PrefixOptionNode != null) {
					return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)PrefixOptionNode);
				}
				// Match Command Symbol
				ZNode PipedNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPattern.PatternName, ZTokenContext._Required);
				if(PipedNode.IsErrorNode()) {
					return PipedNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)PipedNode);
			}
			// Match Redirect
			ZNode RedirectNode = TokenContext.ParsePattern(ParentNode, RedirectPattern.PatternName, ZTokenContext._Optional);
			if(RedirectNode != null) {
				((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)RedirectNode);
				continue;
			}
			// Match Suffix Option
			ZNode SuffixOptionNode = TokenContext.ParsePattern(ParentNode, SuffixOptionPattern.PatternName, ZTokenContext._Optional);
			if(SuffixOptionNode != null) {
				if(SuffixOptionNode.IsErrorNode()) {
					return SuffixOptionNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)SuffixOptionNode);
			}
			// Match Argument
			ZNode ArgNode = TokenContext.ParsePattern(ParentNode, CommandArgPattern.PatternName, ZTokenContext._Optional);
			if(ArgNode == null) {
				break;
			}
			CommandNode.Set(ZNode._AppendIndex, ArgNode);
		}
		return CommandNode;
	}
}
