package dshell.ast;

import java.util.ArrayList;

import dshell.lang.ModifiedTypeInfer;
import zen.ast.ZCatchNode;
import zen.ast.ZNode;
import zen.codegen.jvm.ModifiedJavaByteCodeGenerator;
import zen.codegen.jvm.ModifiedReflectionEngine;
import zen.parser.ZVisitor;

public class DShellTryNode extends ZNode {
	public ZNode TryNode = null;
	public ArrayList<ZNode> CatchNodeList;
	public ZNode FinallyNode = null;

	public DShellTryNode(ZNode ParentNode) {
		super(ParentNode, null);
		this.CatchNodeList = new ArrayList<ZNode>();
	}

	@Override public void Append(ZNode Node) {
		this.SetChild(Node);
		if(Node instanceof ZCatchNode) {
			this.CatchNodeList.add(Node);
		}
		else if(this.TryNode == null) {
			this.TryNode = Node;
		}
		else {
			this.FinallyNode = Node;
		}
	}
	@Override public void Accept(ZVisitor Visitor) {
		if(Visitor instanceof ModifiedTypeInfer) {
			((ModifiedTypeInfer)Visitor).VisitTryNode(this);
		}
		else if(Visitor instanceof ModifiedJavaByteCodeGenerator) {
			((ModifiedJavaByteCodeGenerator)Visitor).VisitTryNode(this);
		}
		else if(Visitor instanceof ModifiedReflectionEngine) {
			((ModifiedReflectionEngine)Visitor).VisitTryNode(this);
		}
		else {
			throw new RuntimeException(Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
