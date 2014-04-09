package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.parser.BVisitor;

public class DShellTryNode extends AbstractListNode {
	public final static int _Try = 0;
	public final static int _Finally = 1;

	public DShellTryNode(BNode ParentNode) {
		super(ParentNode, null, 2);
	}

	public final BunBlockNode TryBlockNode() {
		BNode BlockNode = this.AST[_Try];
		if(BlockNode instanceof BunBlockNode) {
			return (BunBlockNode) BlockNode;
		}
		Utils.fatal(1, "need ZBlockNode: " + BlockNode);
		return null;
	}

	public final boolean HasFinallyBlockNode() {
		return this.AST[_Finally] != null;
	}

	public final BunBlockNode FinallyBlockNode() {
		BNode BlockNode = this.AST[_Finally];
		if(BlockNode instanceof BunBlockNode) {
			return (BunBlockNode) BlockNode;
		}
		Utils.fatal(1, "need ZBlockNode: " + BlockNode);
		return null;
	}

	@Override public void Accept(BVisitor Visitor) {
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitTryNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
