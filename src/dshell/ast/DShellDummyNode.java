package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import zen.ast.ZNode;
import zen.parser.ZVisitor;
import zen.type.ZType;

public class DShellDummyNode extends ZNode {
	public DShellDummyNode(ZNode ParentNode) {
		super(ParentNode, null, 0);
		this.Type = ZType.VoidType;
	}
	// do nothing

	@Override public void Accept(ZVisitor Visitor) {
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitDummyNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
