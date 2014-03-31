package dshell.grammar;

import dshell.ast.sugar.DShellArgNode;
import dshell.ast.sugar.DShellCommandNode;
import dshell.lang.DShellGrammar;
import libbun.parser.ast.ZNode;
import libbun.util.LibZen;
import libbun.util.Var;
import libbun.util.ZMatchFunction;
import libbun.parser.ZPatternToken;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class PrefixOptionPatternFunc extends ZMatchFunction {
	public final static String _PatternName = "$PrefixOption$";

	@Override public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZToken Token = TokenContext.GetToken(ZTokenContext._MoveNext);
		@Var String Symbol = Token.GetText();
		if(Symbol.equals(DShellGrammar.trace)) {
			@Var ZNode CommandNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunc._PatternName, ZTokenContext._Required);
			if(CommandNode.IsErrorNode()) {
				return CommandNode;
			}
			@Var DShellCommandNode Node = new DShellCommandNode(ParentNode, Token, Symbol);
			return Node.AppendPipedNextNode((DShellCommandNode) CommandNode);
		}
		if(Symbol.equals(DShellGrammar.timeout) && LeftNode == null) {
			@Var ZNode TimeNode = this.ParseTimeout(ParentNode, TokenContext);
			if(TimeNode.IsErrorNode()) {
				return TimeNode;
			}
			@Var ZNode CommandNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunc._PatternName, ZTokenContext._Required);
			if(CommandNode.IsErrorNode()) {
				return CommandNode;
			}
			@Var DShellCommandNode Node = new DShellCommandNode(ParentNode, Token, Symbol);
			Node.AppendArgNode(TimeNode);
			return Node.AppendPipedNextNode((DShellCommandNode) CommandNode);
		}
		return null;
	}

	public ZNode ParseTimeout(ZNode ParentNode, ZTokenContext TokenContext) {
		@Var ZToken NumToken = TokenContext.GetToken(ZTokenContext._MoveNext);
		if((NumToken instanceof ZPatternToken)) {
			if(((ZPatternToken)NumToken).PresetPattern.PatternName.equals(("$IntegerLiteral$"))) {
				@Var long Num = LibZen._ParseInt(NumToken.GetText());
				if(Num > 0) {
					if(NumToken.IsNextWhiteSpace()) {
						return new DShellArgNode(ParentNode, Long.toString(Num));
					}
					@Var ZToken UnitToken = TokenContext.GetToken(ZTokenContext._MoveNext);
					@Var String UnitSymbol = UnitToken.GetText();
					if(UnitSymbol.equals("ms")) {
						return new DShellArgNode(ParentNode, Long.toString(Num));
					}
					if(UnitSymbol.equals("s")) {
						return new DShellArgNode(ParentNode, Long.toString(Num * 1000));
					}
					if(UnitSymbol.equals("m")) {
						return new DShellArgNode(ParentNode, Long.toString(Num * 1000 * 60));
					}
					return TokenContext.CreateExpectedErrorNode(UnitToken, "{ms, s, m}");
				}
			}
		}
		return TokenContext.CreateExpectedErrorNode(NumToken, "Integer Number Symbol");
	}
}
