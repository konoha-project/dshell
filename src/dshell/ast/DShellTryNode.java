package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BBlockNode;
import libbun.ast.BListNode;
import libbun.ast.BNode;
import libbun.parser.BVisitor;

public class DShellTryNode extends BListNode {
	public final static int _Try = 0;
	public final static int _Finally = 1;

	public DShellTryNode(BNode ParentNode) {
		super(ParentNode, null, 2);
	}

	public final BBlockNode TryBlockNode() {
		BNode BlockNode = this.AST[_Try];
		if(BlockNode instanceof BBlockNode) {
			return (BBlockNode) BlockNode;
		}
		Utils.fatal(1, "need ZBlockNode: " + BlockNode);
		return null;
	}

	public final boolean HasFinallyBlockNode() {
		return this.AST[_Finally] != null;
	}

	public final BBlockNode FinallyBlockNode() {
		BNode BlockNode = this.AST[_Finally];
		if(BlockNode instanceof BBlockNode) {
			return (BBlockNode) BlockNode;
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
