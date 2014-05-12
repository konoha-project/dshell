package dshell.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.classic.LibBunTypeChecker;
import libbun.type.BType;

public class DShellImportEnvNode extends SyntaxSugarNode {
	public final static int _NameInfo = 0;

	public DShellImportEnvNode(BNode parentNode) {
		super(parentNode, 1);
	}

	@Override
	public void PerformTyping(LibBunTypeChecker typeChecker, BType contextType) {
	}	// do nothing

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker typeChecker) {
		String envName = this.AST[_NameInfo].SourceToken.GetText();
		BNode letNode = new BunLetVarNode(this, BunLetVarNode._IsReadOnly, null, null);
		letNode.SetNode(BunLetVarNode._NameInfo, this.AST[_NameInfo]);
		GetNameNode funcNameNode = new GetNameNode(this.ParentNode, this.SourceToken, "getEnv");
		FuncCallNode getEnvNode = new FuncCallNode(this.ParentNode, funcNameNode);
		getEnvNode.SourceToken = SourceToken;
		getEnvNode.SetNode(BNode._AppendIndex, new BunStringNode(getEnvNode, null, envName));
		letNode.SetNode(BunLetVarNode._InitValue, getEnvNode);
		return new DesugarNode(this, letNode);
	}
}
