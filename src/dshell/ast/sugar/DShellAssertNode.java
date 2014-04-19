package dshell.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetNameNode;
import libbun.encode.LibBunGenerator;
import libbun.parser.LibBunTypeChecker;

public class DShellAssertNode extends SyntaxSugarNode {
	public final static int _Expr = 0;

	public DShellAssertNode(BNode ParentNode) {
		super(ParentNode, 1);
	}

	@Override
	public DesugarNode DeSugar(LibBunGenerator Generator, LibBunTypeChecker TypeChecker) {
		GetNameNode FuncNameNode = new GetNameNode(this, this.SourceToken, "assertDShell");
		FuncCallNode Node = new FuncCallNode(this.ParentNode, FuncNameNode);
		Node.SourceToken = this.SourceToken;
		Node.SetNode(BNode._AppendIndex, this.AST[_Expr]);
		return new DesugarNode(this, Node);
	}
}
