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
		String Command = ((ZStringNode)ParentNode.GetNameSpace().GetSymbolNode(DShellGrammar.CommandSymbol(CommandToken.GetText()))).StringValue;
		if(Command == null) {
			return new ZErrorNode(ParentNode, CommandToken, "undefined command symbol");
		}
		ZNode CommandNode = new DShellCommandNode(ParentNode, CommandToken);
		CommandNode.Set(ZNode._AppendIndex, new ZStringNode(ParentNode, CommandToken, Command));
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
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
			ZNode OptionNode = TokenContext.ParsePattern(ParentNode, "$SuffixOption$", ZTokenContext.Optional);
			if(OptionNode != null) {
				if(OptionNode instanceof ZErrorNode) {
					return OptionNode;
				}
				return ((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)OptionNode);
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
