package dshell.grammar;

import zen.ast.ZNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class DShellImportPattern extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		TokenContext.MoveNext();
		ZToken Token = TokenContext.GetToken();
		if(Token.EqualsText("command")) {
			return TokenContext.ParsePattern(ParentNode, ImportCommandPattern.PatternName, ZTokenContext._Required);
		}
		if(Token.EqualsText("env")) {
			return TokenContext.ParsePattern(ParentNode, ImportEnvPattern.PatternName, ZTokenContext._Required);
		}
		return null;
	}
}
