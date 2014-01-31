package dshell.ast;

import java.util.ArrayList;

import zen.codegen.jvm.ModifiedJavaByteCodeGenerator;
import zen.codegen.jvm.ModifiedReflectionEngine;
import dshell.lang.ModifiedTypeInfer;

import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.deps.Field;
import zen.parser.ZVisitor;

public class DShellCommandNode extends ZNode {
	@Field public ArrayList<ZNode> ArgumentList; // ["ls", "-la"]
	@Field public ZNode PipedNextNode;

	public DShellCommandNode(ZStringNode Node) {
		super();
		this.ArgumentList = new ArrayList<ZNode>();
		this.ArgumentList.add(this.SetChild(Node));
		this.PipedNextNode = null;
	}

	@Override public void Append(ZNode Node) {
		this.ArgumentList.add(this.SetChild(Node));
	}

	public ZNode AppendPipedNextNode(DShellCommandNode Node) {
		this.PipedNextNode = this.SetChild(Node);
		return this;
	}

	public ZNode AppendOptionNode(DShellCommandNode Node) {
		DShellCommandNode CurrentNode = this;
		while(CurrentNode.PipedNextNode != null) {
			CurrentNode = (DShellCommandNode) CurrentNode.PipedNextNode;
		}
		CurrentNode.AppendPipedNextNode(Node);
		return this;
	}

	@Override public void Accept(ZVisitor Visitor) {
		if(Visitor instanceof ModifiedJavaByteCodeGenerator) {
			((ModifiedJavaByteCodeGenerator)Visitor).VisitCommandNode(this);
		}
		else if(Visitor instanceof ModifiedTypeInfer) {
			((ModifiedTypeInfer)Visitor).VisitCommandNode(this);
		}
		else if(Visitor instanceof ModifiedReflectionEngine) {
			((ModifiedReflectionEngine)Visitor).VisitCommandNode(this);
		}
		else {
			throw new RuntimeException(Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}