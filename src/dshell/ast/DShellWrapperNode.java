package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BNode;
import libbun.ast.decl.BunClassNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.parser.BVisitor;

public class DShellWrapperNode extends BNode {
	private BNode TargetNode;
	private boolean isVarTarget = false;

	public DShellWrapperNode(BNode TargetNode) {
		super(TargetNode.ParentNode, TargetNode.SourceToken, 1);
		if(TargetNode instanceof BunClassNode || TargetNode instanceof BunFunctionNode) {
			this.TargetNode = TargetNode;
			return;
		}
		if(TargetNode instanceof BunLetVarNode) {
			this.TargetNode = TargetNode;
			this.isVarTarget = true;
			return;
		}
		Utils.fatal(1, TargetNode.getClass().getName() + " is unsupported Node");
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
	public void Accept(BVisitor Visitor) {
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitWrapperNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
