package libbun.lang.bun.shell;

import libbun.ast.BNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.ast.sugar.DShellImportEnvNode;

public class ImportEnvPatternFunc extends BMatchFunction {
	public final static String PatternName = "$ImportEnv$";
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new DShellImportEnvNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "env", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, DShellImportEnvNode._NameInfo, "$Name$", BTokenContext._Required);
		if(Node.IsErrorNode()) {
			return Node;
		}
		BNode LetNode = ((SyntaxSugarNode)Node).DeSugar(TokenContext.Generator, TokenContext.Generator.TypeChecker).AST[0];
		if(LetNode instanceof BunLetVarNode && !ParentNode.IsTopLevel()) {
			return new BunVarBlockNode(ParentNode, (BunLetVarNode) LetNode);
		}
		return LetNode;
	}
}
