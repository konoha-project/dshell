package dshell.ast;

import dshell.lang.ModifiedTypeSafer;
import dshell.lib.Utils;
import zen.ast.ZBlockNode;
import zen.ast.ZListNode;
import zen.ast.ZNode;
import zen.codegen.jvm.ModifiedAsmGenerator;
import zen.codegen.jvm.ModifiedJavaEngine;
import zen.parser.ZVisitor;

public class DShellTryNode extends ZListNode {
	public final static int _Try = 0;
	public final static int _Finally = 1;

	public DShellTryNode(ZNode ParentNode) {
		super(ParentNode, null, 2);
	}

	public final ZBlockNode TryBlockNode() {
		ZNode BlockNode = this.AST[_Try];
		if(BlockNode instanceof ZBlockNode) {
			return (ZBlockNode) BlockNode;
		}
		Utils.fatal(1, "need ZBlockNode: " + BlockNode);
		return null;
	}

	public final boolean HasFinallyBlockNode() {
		return this.AST[_Finally] != null;
	}

	public final ZBlockNode FinallyBlockNode() {
		ZNode BlockNode = this.AST[_Finally];
		if(BlockNode instanceof ZBlockNode) {
			return (ZBlockNode) BlockNode;
		}
		Utils.fatal(1, "need ZBlockNode: " + BlockNode);
		return null;
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
