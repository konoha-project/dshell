package dshell.grammar;

import libbun.parser.ast.ZNode;
import libbun.util.ZMatchFunction;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class DShellImportPatternFunc extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		TokenContext.MoveNext();
		ZToken Token = TokenContext.GetToken();
		if(Token.EqualsText("command")) {
			return TokenContext.ParsePattern(ParentNode, ImportCommandPatternFunc._PatternName, ZTokenContext._Required);
		}
		if(Token.EqualsText("env")) {
			return TokenContext.ParsePattern(ParentNode, ImportEnvPatternFunc.PatternName, ZTokenContext._Required);
		}
		return null;
	}
}
