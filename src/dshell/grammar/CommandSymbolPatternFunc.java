package dshell.grammar;

import dshell.ast.sugar.DShellCommandNode;
import dshell.lang.DShellGrammar;
import libbun.parser.ast.ZErrorNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.util.Var;
import libbun.util.ZMatchFunction;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class CommandSymbolPatternFunc extends ZMatchFunction {
	public final static String _PatternName = "$CommandSymbol$";

	@Override public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZToken CommandToken = TokenContext.GetToken(ZTokenContext._MoveNext);
		@Var ZNode SymbolNode = ParentNode.GetNameSpace().GetSymbol(DShellGrammar.toCommandSymbol(CommandToken.GetText()));
		if(SymbolNode == null || !(SymbolNode instanceof ZStringNode)) {
			return new ZErrorNode(ParentNode, CommandToken, "undefined command symbol");
		}
		@Var String Command = ((ZStringNode)SymbolNode).StringValue;
		@Var DShellCommandNode CommandNode = new DShellCommandNode(ParentNode, CommandToken, Command);
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
				// Match Prefix Option
				@Var ZNode PrefixOptionNode = TokenContext.ParsePatternAfter(ParentNode, CommandNode, PrefixOptionPatternFunc._PatternName, ZTokenContext._Optional);
				if(PrefixOptionNode != null) {
					return CommandNode.AppendPipedNextNode((DShellCommandNode)PrefixOptionNode);
				}
				// Match Command Symbol
				@Var ZNode PipedNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunc._PatternName, ZTokenContext._Required);
				if(PipedNode.IsErrorNode()) {
					return PipedNode;
				}
				return CommandNode.AppendPipedNextNode((DShellCommandNode)PipedNode);
			}
			// Match Redirect
			@Var ZNode RedirectNode = TokenContext.ParsePattern(ParentNode, RedirectPatternFunc._PatternName, ZTokenContext._Optional);
			if(RedirectNode != null) {
				CommandNode.AppendPipedNextNode((DShellCommandNode)RedirectNode);
				continue;
			}
			// Match Suffix Option
			@Var ZNode SuffixOptionNode = TokenContext.ParsePattern(ParentNode, SuffixOptionPatternFunc._PatternName, ZTokenContext._Optional);
			if(SuffixOptionNode != null) {
				if(SuffixOptionNode.IsErrorNode()) {
					return SuffixOptionNode;
				}
				return CommandNode.AppendPipedNextNode((DShellCommandNode)SuffixOptionNode);
			}
			// Match Argument
			@Var ZNode ArgNode = TokenContext.ParsePattern(ParentNode, CommandArgPatternFunc._PatternName, ZTokenContext._Optional);
			if(ArgNode == null) {
				break;
			}
			CommandNode.AppendArgNode(ArgNode);
		}
		return CommandNode;
	}
}
