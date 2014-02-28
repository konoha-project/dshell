package dshell.grammar;

import dshell.ast.DShellCommandNode;
import dshell.lang.DShellGrammar;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.deps.LibZen;
import zen.deps.ZMatchFunction;
import zen.parser.ZPatternToken;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class PrefixOptionPattern extends ZMatchFunction {
	public final static String PatternName = "$PrefixOption$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		String Symbol = Token.GetText();
		if(Symbol.equals(DShellGrammar.trace)) {
			ZNode CommandNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPattern.PatternName, ZTokenContext._Required);
			if(CommandNode.IsErrorNode()) {
				return CommandNode;
			}
			ZNode Node = new DShellCommandNode(ParentNode, Token);
			Node.Set(ZNode._AppendIndex, new ZStringNode(ParentNode, Token, Symbol));
			return ((DShellCommandNode)Node).AppendPipedNextNode((DShellCommandNode) CommandNode);
		}
		if(Symbol.equals(DShellGrammar.timeout) && LeftNode == null) {
			ZNode TimeNode = this.ParseTimeout(ParentNode, TokenContext);
			if(TimeNode.IsErrorNode()) {
				return TimeNode;
			}
			ZNode CommandNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPattern.PatternName, ZTokenContext._Required);
			if(CommandNode.IsErrorNode()) {
				return CommandNode;
			}
			ZNode Node = new DShellCommandNode(ParentNode, Token);
			Node.Set(ZNode._AppendIndex, new ZStringNode(ParentNode, Token, Symbol));
			Node.Set(ZNode._AppendIndex, TimeNode);
			return ((DShellCommandNode)Node).AppendPipedNextNode((DShellCommandNode) CommandNode);
		}
		return null;
	}

	public ZNode ParseTimeout(ZNode ParentNode, ZTokenContext TokenContext) {
		ZToken NumToken = TokenContext.GetToken(ZTokenContext._MoveNext);
		if((NumToken instanceof ZPatternToken)) {
			if(((ZPatternToken)NumToken).PresetPattern.PatternName.equals(("$IntegerLiteral$"))) {
				long Num = LibZen._ParseInt(NumToken.GetText());
				if(Num > 0) {
					if(NumToken.IsNextWhiteSpace()) {
						return new ZStringNode(ParentNode, NumToken, Long.toString(Num));
					}
					ZToken UnitToken = TokenContext.GetToken(ZTokenContext._MoveNext);
					String UnitSymbol = UnitToken.GetText();
					if(UnitSymbol.equals("ms")) {
						return new ZStringNode(ParentNode, NumToken, Long.toString(Num));
					}
					if(UnitSymbol.equals("s")) {
						return new ZStringNode(ParentNode, NumToken, Long.toString(Num * 1000));
					}
					if(UnitSymbol.equals("m")) {
						return new ZStringNode(ParentNode, NumToken, Long.toString(Num * 1000 * 60));
					}
					return TokenContext.CreateExpectedErrorNode(UnitToken, "{ms, s, m}");
				}
			}
		}
		return TokenContext.CreateExpectedErrorNode(NumToken, "Integer Number Symbol");
	}
}
