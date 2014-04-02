package dshell.grammar;

import libbun.lang.bun.shell.ImportPatternFunction;
import libbun.parser.ast.ZNode;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class DShellImportPatternFunc extends ImportPatternFunction {
	@Override
	public ZNode MatchEnvPattern(ZNode ParentNode, ZTokenContext TokenContext, ZToken Token) {
		if(Token.EqualsText("env")) {
			return TokenContext.ParsePattern(ParentNode, ImportEnvPatternFunc.PatternName, ZTokenContext._Required);
		}
		return null;
	}
}
