package dshell.internal.ast;

import dshell.internal.lang.DShellVisitor;
import dshell.internal.lib.Utils;
import libbun.ast.BNode;
import libbun.parser.classic.LibBunVisitor;

public class DShellWrapperNode extends BNode {
	private BNode targetNode;
	private final boolean isVarTarget;

	public DShellWrapperNode(BNode targetNode) {
		this(targetNode, false);
	}

	public DShellWrapperNode(BNode targetNode, boolean isVarTarget) {
		super(targetNode.ParentNode, 1);
		this.SourceToken = targetNode.SourceToken;
		this.targetNode = targetNode;
		this.isVarTarget = isVarTarget;
	}

	public BNode getTargetNode() {
		return this.targetNode;
	}

	public void setTargetNode(BNode Node) {
		this.targetNode = Node;
	}

	public boolean isVarTarget() {
		return this.isVarTarget;
	}

	@Override
	public void Accept(LibBunVisitor visitor) {
		if(visitor instanceof DShellVisitor) {
			((DShellVisitor)visitor).visitWrapperNode(this);
		}
		else {
			Utils.fatal(1, visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
