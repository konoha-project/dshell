package dshell.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.classic.LibBunTypeChecker;
import libbun.type.BType;

public class ArgumentNode extends SyntaxSugarNode {
	public final static int _Expr = 0;
	// arg type
	public final static int _Normal = 0;
	public final static int _Substitution = 1;
	private final static String[] _funcNames = {"createCommandArg", "createSubstitutedArg"};

	public final int argType;

	public ArgumentNode(BNode parentNode, int argType) {
		super(parentNode, 1);
		this.argType = argType;
	}

	public ArgumentNode(BNode parentNode, String value) {
		this(parentNode, _Normal);
		this.SetNode(_Expr, new BunStringNode(this, null, value));
	}

	@Override
	public void PerformTyping(LibBunTypeChecker typeChecker, BType contextType) {
		typeChecker.CheckTypeAt(this, _Expr, BType.StringType);
	}

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker typeChekcer) {
		BNode node = new FuncCallNode(this, new GetNameNode(this, null, _funcNames[this.argType]));
		node.SetNode(BNode._AppendIndex, this.AST[_Expr]);
		return new DesugarNode(this, node);
	}
}
