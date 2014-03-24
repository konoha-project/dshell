package dshell.ast.sugar;

import zen.ast.ZDesugarNode;
import zen.ast.ZFuncCallNode;
import zen.ast.ZGetNameNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.ast.ZSugarNode;
import zen.parser.ZGenerator;
import zen.parser.ZTypeChecker;

public class DShellArgNode extends ZSugarNode {
	public final static int _Expr = 0;
	// arg type
	public final static int __normal = 0;
	public final static int __substitution = 1;
	private final static String[] funcNames = {"createCommandArg", "createSubstitutedArg"};

	private final int ArgType;

	public DShellArgNode(ZNode ParentNode, int ArgType) {
		super(ParentNode, null, 1);
		this.ArgType = ArgType;
	}

	public DShellArgNode(ZNode ParentNode, String Value) {
		this(ParentNode, __normal);
		this.SetNode(_Expr, new ZStringNode(this, null, Value));
	}

	@Override
	public ZDesugarNode DeSugar(ZGenerator Generator, ZTypeChecker TypeChekcer) {
		ZNode Node = new ZFuncCallNode(this, new ZGetNameNode(this, null, funcNames[this.ArgType]));
		Node.SetNode(ZNode._AppendIndex, this.AST[_Expr]);
		return new ZDesugarNode(this, Node);
	}
}
