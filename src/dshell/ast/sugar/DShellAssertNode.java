package dshell.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetNameNode;
import libbun.encode.AbstractGenerator;
import libbun.parser.BTypeChecker;

public class DShellAssertNode extends SyntaxSugarNode {
	public final static int _Expr = 0;

	public DShellAssertNode(BNode ParentNode) {
		super(ParentNode, null, 1);
	}

	@Override
	public DesugarNode DeSugar(AbstractGenerator Generator, BTypeChecker TypeChecker) {
		GetNameNode FuncNameNode = new GetNameNode(this, this.SourceToken, "assertDShell");
		FuncCallNode Node = new FuncCallNode(this.ParentNode, FuncNameNode);
		Node.SourceToken = this.SourceToken;
		Node.SetNode(BNode._AppendIndex, this.AST[_Expr]);
		return new DesugarNode(this, Node);
	}
}
