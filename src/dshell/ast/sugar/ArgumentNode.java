package dshell.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.LibBunTypeChecker;
import libbun.type.BType;

public class ArgumentNode extends SyntaxSugarNode {
	public final static int _Expr = 0;
	// arg type
	public final static int _Normal = 0;
	public final static int _Substitution = 1;
	private final static String[] _funcNames = {"createCommandArg", "createSubstitutedArg"};

	public final int ArgType;

	public ArgumentNode(BNode ParentNode, int ArgType) {
		super(ParentNode, 1);
		this.ArgType = ArgType;
	}

	public ArgumentNode(BNode ParentNode, String Value) {
		this(ParentNode, _Normal);
		this.SetNode(_Expr, new BunStringNode(this, null, Value));
	}

	@Override public void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType) {
		TypeChecker.CheckTypeAt(this, _Expr, BType.StringType);
	}

	@Override public DesugarNode PerformDesugar(LibBunTypeChecker TypeChekcer) {
		BNode Node = new FuncCallNode(this, new GetNameNode(this, null, _funcNames[this.ArgType]));
		Node.SetNode(BNode._AppendIndex, this.AST[_Expr]);
		return new DesugarNode(this, Node);
	}
}
