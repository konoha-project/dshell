package dshell.grammar;

import libbun.ast.BNode;
import libbun.lang.bun.shell.ImportPatternFunction;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;

public class DShellImportPatternFunc extends ImportPatternFunction {
	@Override
	public BNode MatchEnvPattern(BNode ParentNode, BTokenContext TokenContext, BToken Token) {
		if(Token.EqualsText("env")) {
			return TokenContext.ParsePattern(ParentNode, ImportEnvPatternFunc.PatternName, BTokenContext._Required);
		}
		return null;
	}
}
