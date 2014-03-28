package dshell.ast.sugar;

import libbun.parser.ast.ZDesugarNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZSugarNode;
import libbun.parser.ZGenerator;
import libbun.parser.ZMacroFunc;
import libbun.parser.ZTypeChecker;
import libbun.type.ZType;

public class DShellAssertNode extends ZSugarNode {
	public final static int _Expr = 0;

	public DShellAssertNode(ZNode ParentNode) {
		super(ParentNode, null, 1);
	}

	@Override
	public ZDesugarNode DeSugar(ZGenerator Generator, ZTypeChecker TypeChecker) {
		ZMacroFunc Func = Generator.GetMacroFunc("assertDShell", ZType.BooleanType, 1);
		ZNode FuncNode = TypeChecker.CreateDefinedFuncCallNode(this.ParentNode, this.SourceToken, Func);
		FuncNode.SetNode(ZNode._AppendIndex, this.AST[_Expr]);
		return new ZDesugarNode(this, FuncNode);
	}
}
