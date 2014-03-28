package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.parser.ast.ZBlockNode;
import libbun.parser.ast.ZListNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ZVisitor;

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
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitTryNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
