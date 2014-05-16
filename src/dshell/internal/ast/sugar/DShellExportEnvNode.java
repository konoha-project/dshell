package dshell.internal.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.classic.LibBunTypeChecker;
import libbun.type.BType;

public class DShellExportEnvNode extends SyntaxSugarNode {
	public final static int _NameInfo = 0;
	public final static int _Expr = 1;

	public DShellExportEnvNode(BNode parentNode) {
		super(parentNode, 2);
	}

	@Override
	public void PerformTyping(LibBunTypeChecker typeChecker, BType contextType) {
		typeChecker.CheckTypeAt(this, _Expr, BType.StringType);
	}

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker typeChecker) {
		String envName = this.AST[_NameInfo].SourceToken.GetText();
		GetNameNode funcNameNode = new GetNameNode(this.ParentNode, this.SourceToken, "setEnv");
		FuncCallNode setEnvNode = new FuncCallNode(this.ParentNode, funcNameNode);
		setEnvNode.SourceToken = this.SourceToken;
		setEnvNode.SetNode(BNode._AppendIndex, new BunStringNode(setEnvNode, null, envName));
		setEnvNode.SetNode(BNode._AppendIndex, this.AST[DShellExportEnvNode._Expr]);
		BNode letNode = new BunLetVarNode(this, BunLetVarNode._IsReadOnly, null, null);
		letNode.SetNode(BunLetVarNode._NameInfo, this.AST[_NameInfo]);
		letNode.SetNode(BunLetVarNode._InitValue, setEnvNode);
		return new DesugarNode(this, letNode);
	}
}
