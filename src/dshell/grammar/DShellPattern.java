package dshell.grammar;

import dshell.ast.DShellCommandNode;
import dshell.lang.DShellGrammar;
import zen.ast.ZErrorNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZSource;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class DShellPattern extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken CommandToken = GetJoinedCommandToken(TokenContext);
		if(CommandToken.IsNull()) {
			return TokenContext.CreateExpectedErrorNode(CommandToken, "$DShell$");
		}
		ZNode SymbolNode = ParentNode.GetNameSpace().GetSymbolNode(DShellGrammar.CommandSymbol(CommandToken.GetText()));
		if(SymbolNode == null) {
			return new ZErrorNode(ParentNode, CommandToken, "undefined command symbol");
		}
		String Command = ((ZStringNode)SymbolNode).StringValue;
		ZNode CommandNode = new DShellCommandNode(ParentNode, CommandToken);
		CommandNode.Set(ZNode._AppendIndex, new ZStringNode(ParentNode, CommandToken, Command));
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
				// Match Prefix Option
				ZNode PrefixOptionNode = TokenContext.ParsePatternAfter(ParentNode, CommandNode, "$PrefixOption$", ZTokenContext.Optional);
				if(PrefixOptionNode != null) {
					return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)PrefixOptionNode);
				}
				// Match DShell
				ZNode PipedNode = TokenContext.ParsePattern(ParentNode, "$DShell$", ZTokenContext.Required);
				if(PipedNode.IsErrorNode()) {
					return PipedNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)PipedNode);
			}
			// Match Redirect
			ZNode RedirectNode = TokenContext.ParsePattern(ParentNode, "$Redirect$", ZTokenContext.Optional);
			if(RedirectNode != null) {
				((DShellCommandNode)CommandNode).AppendOptionNode((DShellCommandNode)RedirectNode);
				continue;
			}
			// Match Suffix Option
			ZNode SuffixOptionNode = TokenContext.ParsePattern(ParentNode, "$SuffixOption$", ZTokenContext.Optional);
			if(SuffixOptionNode != null) {
				if(SuffixOptionNode instanceof ZErrorNode) {
					return SuffixOptionNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)SuffixOptionNode);
			}
			// Match Argument
			ZNode ArgNode = TokenContext.ParsePattern(ParentNode, "$CommandArg$", ZTokenContext.Optional);
			if(ArgNode == null) {
				break;
			}
			CommandNode.Set(ZNode._AppendIndex, ArgNode);
		}
		return CommandNode;
	}

	private ZToken GetJoinedCommandToken(ZTokenContext TokenContext) {
		if(!TokenContext.HasNext()) {
			return TokenContext.GetToken();
		}
		ZToken Token = TokenContext.GetToken();
		String CommandSymbol = Token.GetText();
		TokenContext.MoveNext();
		while(!DShellGrammar.IsNextWhiteSpace(Token)) {
			if(DShellGrammar.MatchStopToken(TokenContext)) {
				break;
			}
			Token = TokenContext.GetToken();
			CommandSymbol += Token.GetText();
			TokenContext.MoveNext();
		}
		ZSource CommandSource = new ZSource(Token.GetFileName(), Token.GetLineNumber(), CommandSymbol, TokenContext);
		return new ZToken(CommandSource, 0, CommandSymbol.length());
	}
}
