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

public class DShellExportEnvNode extends SyntaxSugarNode {
	public final static int _NameInfo = 0;
	public final static int _Expr = 1;

	public DShellExportEnvNode(BNode ParentNode) {
		super(ParentNode, 2);
	}

	@Override
	public void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType) {
		TypeChecker.CheckTypeAt(this, _Expr, BType.StringType);
	}

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker TypeChekcer) {
		String EnvName = this.AST[_NameInfo].SourceToken.GetText();
		GetNameNode FuncNameNode = new GetNameNode(this.ParentNode, this.SourceToken, "setEnv");
		FuncCallNode SetEnvNode = new FuncCallNode(this.ParentNode, FuncNameNode);
		SetEnvNode.SourceToken = this.SourceToken;
		SetEnvNode.SetNode(BNode._AppendIndex, new BunStringNode(SetEnvNode, null, EnvName));
		SetEnvNode.SetNode(BNode._AppendIndex, this.AST[DShellExportEnvNode._Expr]);
		BNode LetNode = new BunLetVarNode(this, BunLetVarNode._IsReadOnly, null, null);
		LetNode.SetNode(BunLetVarNode._NameInfo, this.AST[_NameInfo]);
		LetNode.SetNode(BunLetVarNode._InitValue, SetEnvNode);
		return new DesugarNode(this, LetNode);
	}
}
