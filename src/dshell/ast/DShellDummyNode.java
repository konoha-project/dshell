package dshell.ast;

import dshell.lang.ModifiedTypeSafer;
import zen.ast.ZNode;
import zen.parser.ZVisitor;

public class DShellDummyNode extends ZNode {
	public DShellDummyNode(ZNode ParentNode) {
		super(ParentNode, null, 0);
	}
	// do nothing

	@Override public void Accept(ZVisitor Visitor) {
		if(Visitor instanceof ModifiedTypeSafer) {
			((ModifiedTypeSafer)Visitor).VisitDummyNode(this);
		}
	}
}
