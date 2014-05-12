package dshell.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetNameNode;
import libbun.parser.classic.LibBunTypeChecker;
import libbun.type.BType;

public class DShellAssertNode extends SyntaxSugarNode {
	public final static int _Expr = 0;

	public DShellAssertNode(BNode parentNode) {
		super(parentNode, 1);
	}

	@Override
	public void PerformTyping(LibBunTypeChecker typeChecker, BType contextType) {
		typeChecker.CheckTypeAt(this, _Expr, BType.BooleanType);
	}

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker typeChecker) {
		GetNameNode funcNameNode = new GetNameNode(this, this.SourceToken, "assertDShell");
		FuncCallNode node = new FuncCallNode(this.ParentNode, funcNameNode);
		node.SourceToken = this.SourceToken;
		node.SetNode(BNode._AppendIndex, this.AST[_Expr]);
		return new DesugarNode(this, node);
	}
}
