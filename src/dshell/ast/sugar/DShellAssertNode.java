package dshell.ast.sugar;

import libbun.ast.BDesugarNode;
import libbun.ast.BNode;
import libbun.ast.BSugarNode;
import libbun.parser.BGenerator;
import libbun.parser.BTypeChecker;
import libbun.type.BMacroFunc;
import libbun.type.BType;

public class DShellAssertNode extends BSugarNode {
	public final static int _Expr = 0;

	public DShellAssertNode(BNode ParentNode) {
		super(ParentNode, null, 1);
	}

	@Override
	public BDesugarNode DeSugar(BGenerator Generator, BTypeChecker TypeChecker) {
		BMacroFunc Func = Generator.GetMacroFunc("assertDShell", BType.BooleanType, 1);
		BNode FuncNode = TypeChecker.CreateDefinedFuncCallNode(this.ParentNode, this.SourceToken, Func);
		FuncNode.SetNode(BNode._AppendIndex, this.AST[_Expr]);
		return new BDesugarNode(this, FuncNode);
	}
}
