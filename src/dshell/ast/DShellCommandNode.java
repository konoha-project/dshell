package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import zen.ast.ZListNode;
import zen.ast.ZNode;
import zen.parser.ZToken;
import zen.parser.ZVisitor;

public class DShellCommandNode extends ZListNode {
	public ZNode PipedNextNode;

	public DShellCommandNode(ZNode ParentNode, ZToken Token) {
		super(ParentNode, Token, 0);
		this.PipedNextNode = null;
	}

	public ZNode AppendPipedNextNode(DShellCommandNode Node) {
		DShellCommandNode CurrentNode = this;
		while(CurrentNode.PipedNextNode != null) {
			CurrentNode = (DShellCommandNode) CurrentNode.PipedNextNode;
		}
		CurrentNode.PipedNextNode = CurrentNode.SetChild(Node);
		return this;
	}

	@Override public void Accept(ZVisitor Visitor) {
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitCommandNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}