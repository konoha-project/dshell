package dshell.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetNameNode;
import libbun.parser.LibBunTypeChecker;
import libbun.type.BType;

public class DShellAssertNode extends SyntaxSugarNode {
	public final static int _Expr = 0;

	public DShellAssertNode(BNode ParentNode) {
		super(ParentNode, 1);
	}

	@Override
	public void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType) {
		TypeChecker.CheckTypeAt(this, _Expr, BType.BooleanType);
	}

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker TypeChekcer) {
		GetNameNode FuncNameNode = new GetNameNode(this, this.SourceToken, "assertDShell");
		FuncCallNode Node = new FuncCallNode(this.ParentNode, FuncNameNode);
		Node.SourceToken = this.SourceToken;
		Node.SetNode(BNode._AppendIndex, this.AST[_Expr]);
		return new DesugarNode(this, Node);
	}
}
