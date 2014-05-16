package dshell.internal.grammar;

import libbun.ast.BNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.parser.classic.BToken;
import libbun.parser.classic.BTokenContext;
import libbun.util.BMatchFunction;

public class DShellVarPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		int accessFlag = 0;
		BToken token = tokenContext.GetToken();
		if(token.EqualsText("let")) {
			accessFlag = BunLetVarNode._IsReadOnly;
		}
		BNode varNode = new BunLetVarNode(parentNode, accessFlag, null, null);
		varNode = tokenContext.MatchToken(varNode, accessFlag == 0 ? "var" : "let", BTokenContext._Required);
		varNode = tokenContext.MatchPattern(varNode, BunLetVarNode._NameInfo, "$Name$", BTokenContext._Required);
		varNode = tokenContext.MatchPattern(varNode, BunLetVarNode._TypeInfo, "$TypeAnnotation$", BTokenContext._Optional);
		varNode = tokenContext.MatchToken(varNode, "=", BTokenContext._Required);
		varNode = tokenContext.MatchPattern(varNode, BunLetVarNode._InitValue, "$Expression$", BTokenContext._Required);
		return varNode;
	}
}
