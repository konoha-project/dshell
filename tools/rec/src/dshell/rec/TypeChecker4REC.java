package dshell.rec;

import libbun.ast.SyntaxSugarNode;
import libbun.encode.jvm.DShellByteCodeGenerator;
import libbun.type.BType;
import dshell.internal.ast.sugar.DShellAssertNode;
import dshell.internal.lang.DShellTypeChecker;

public class TypeChecker4REC extends DShellTypeChecker {
	public TypeChecker4REC(DShellByteCodeGenerator generator) {
		super(generator);
	}

	@Override
	public void VisitSyntaxSugarNode(SyntaxSugarNode node) {
		if(node instanceof DShellAssertNode) {
			this.VisitAssertNode((DShellAssertNode) node);
		}
		else {
			super.VisitSyntaxSugarNode(node);
		}
	}

	private void VisitAssertNode(DShellAssertNode node) {
		this.CheckTypeAt(node, DShellAssertNode._Expr, BType.BooleanType);
		this.ReturnTypeNode(node, BType.VoidType);
	}
}
