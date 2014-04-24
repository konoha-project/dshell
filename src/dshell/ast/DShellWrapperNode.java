package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BNode;
import libbun.parser.LibBunVisitor;

public class DShellWrapperNode extends BNode {
	private BNode TargetNode;
	private final boolean isVarTarget;

	public DShellWrapperNode(BNode TargetNode) {
		this(TargetNode, false);
	}

	public DShellWrapperNode(BNode TargetNode, boolean isVarTarget) {
		super(TargetNode.ParentNode, 1);
		this.SourceToken = TargetNode.SourceToken;
		this.TargetNode = TargetNode;
		this.isVarTarget = isVarTarget;
	}

	public BNode getTargetNode() {
		return this.TargetNode;
	}

	public void setTargetNode(BNode Node) {
		this.TargetNode = Node;
	}

	public boolean isVarTarget() {
		return this.isVarTarget;
	}

	@Override
	public void Accept(LibBunVisitor Visitor) {
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitWrapperNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
