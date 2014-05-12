package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.parser.classic.LibBunVisitor;

public class DShellTryNode extends AbstractListNode {
	public final static int _Try = 0;
	public final static int _Finally = 1;

	public DShellTryNode(BNode parentNode) {
		super(parentNode, 2);
	}

	public final BunBlockNode tryBlockNode() {
		BNode blockNode = this.AST[_Try];
		if(blockNode instanceof BunBlockNode) {
			return (BunBlockNode) blockNode;
		}
		Utils.fatal(1, "need ZBlockNode: " + blockNode);
		return null;
	}

	public final boolean hasFinallyBlockNode() {
		return this.AST[_Finally] != null;
	}

	public final BunBlockNode finallyBlockNode() {
		BNode blockNode = this.AST[_Finally];
		if(blockNode instanceof BunBlockNode) {
			return (BunBlockNode) blockNode;
		}
		Utils.fatal(1, "need ZBlockNode: " + blockNode);
		return null;
	}

	@Override public void Accept(LibBunVisitor visitor) {
		if(visitor instanceof DShellVisitor) {
			((DShellVisitor)visitor).visitTryNode(this);
		}
		else {
			Utils.fatal(1, visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
