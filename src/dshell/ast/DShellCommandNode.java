package dshell.ast;

import dshell.lang.ModifiedTypeSafer;
import zen.ast.ZListNode;
import zen.ast.ZNode;
import zen.codegen.jvm.ModifiedAsmGenerator;
import zen.codegen.jvm.ModifiedJavaEngine;
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
		if(Visitor instanceof ModifiedAsmGenerator) {
			((ModifiedAsmGenerator)Visitor).VisitCommandNode(this);
		}
		else if(Visitor instanceof ModifiedTypeSafer) {
			((ModifiedTypeSafer)Visitor).VisitCommandNode(this);
		}
		else if(Visitor instanceof ModifiedJavaEngine) {
			((ModifiedJavaEngine)Visitor).VisitCommandNode(this);
		}
		else {
			throw new RuntimeException(Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}