package dshell.rec;

import libbun.ast.SyntaxSugarNode;
import libbun.encode.jvm.DShellByteCodeGenerator;
import libbun.type.BType;
import dshell.ast.sugar.DShellAssertNode;
import dshell.lang.DShellTypeChecker;

public class TypeChecker4REC extends DShellTypeChecker {
	public TypeChecker4REC(DShellByteCodeGenerator Generator) {
		super(Generator);
	}

	@Override
	public void VisitSyntaxSugarNode(SyntaxSugarNode Node) {
		if(Node instanceof DShellAssertNode) {
			this.VisitAssertNode((DShellAssertNode) Node);
		}
		else {
			super.VisitSyntaxSugarNode(Node);
		}
	}

	private void VisitAssertNode(DShellAssertNode Node) {
		this.CheckTypeAt(Node, DShellAssertNode._Expr, BType.BooleanType);
		this.ReturnTypeNode(Node, BType.VoidType);
	}
}
