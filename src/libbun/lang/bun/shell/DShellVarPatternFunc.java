package libbun.lang.bun.shell;

import libbun.ast.BNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;

public class DShellVarPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		int AccessFlag = 0;
		BToken Token = TokenContext.GetToken();
		if(Token.EqualsText("let")) {
			AccessFlag = BunLetVarNode._IsReadOnly;
		}
		BNode VarNode = new BunLetVarNode(ParentNode, AccessFlag, null, null);
		VarNode = TokenContext.MatchToken(VarNode, AccessFlag == 0 ? "var" : "let", BTokenContext._Required);
		VarNode = TokenContext.MatchPattern(VarNode, BunLetVarNode._NameInfo, "$Name$", BTokenContext._Required);
		VarNode = TokenContext.MatchPattern(VarNode, BunLetVarNode._TypeInfo, "$TypeAnnotation$", BTokenContext._Optional);
		VarNode = TokenContext.MatchToken(VarNode, "=", BTokenContext._Required);
		VarNode = TokenContext.MatchPattern(VarNode, BunLetVarNode._InitValue, "$Expression$", BTokenContext._Required);
		return VarNode;
	}
}
