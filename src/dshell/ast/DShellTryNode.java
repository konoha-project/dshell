package dshell.ast;

import dshell.lang.ModifiedTypeSafer;
import zen.ast.ZListNode;
import zen.ast.ZNode;
import zen.codegen.jvm.ModifiedAsmGenerator;
import zen.codegen.jvm.ModifiedJavaEngine;
import zen.parser.ZVisitor;

public class DShellTryNode extends ZListNode {
	public final static int Try = 0;
	public final static int Finally = 1;

	public DShellTryNode(ZNode ParentNode) {
		super(ParentNode, null, 2);
	}

	@Override public void Accept(ZVisitor Visitor) {
		if(Visitor instanceof ModifiedTypeSafer) {
			((ModifiedTypeSafer)Visitor).VisitTryNode(this);
		}
		else if(Visitor instanceof ModifiedAsmGenerator) {
			((ModifiedAsmGenerator)Visitor).VisitTryNode(this);
		}
		else if(Visitor instanceof ModifiedJavaEngine) {
			((ModifiedJavaEngine)Visitor).VisitTryNode(this);
		}
		else {
			throw new RuntimeException(Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
