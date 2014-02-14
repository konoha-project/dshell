package dshell.ast;

import dshell.lang.ModifiedTypeSafer;
import zen.ast.ZNode;
import zen.codegen.jvm.ModifiedAsmGenerator;
import zen.codegen.jvm.ModifiedJavaEngine;
import zen.parser.ZVisitor;

public class DShellDummyNode extends ZNode {
	public DShellDummyNode(ZNode ParentNode) {
		super(ParentNode, null, 0);
	}
	// do nothing

	@Override public void Accept(ZVisitor Visitor) {
		if(Visitor instanceof ModifiedAsmGenerator) {
			((ModifiedAsmGenerator)Visitor).VisitDummyNode(this);
		}
		else if(Visitor instanceof ModifiedTypeSafer) {
			((ModifiedTypeSafer)Visitor).VisitDummyNode(this);
		}
		else if(Visitor instanceof ModifiedJavaEngine) {
			((ModifiedJavaEngine)Visitor).VisitDummyNode(this);
		}
		else {
			throw new RuntimeException(Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
