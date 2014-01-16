package dshell.ast;

import java.util.ArrayList;

import dshell.codegen.javascript.ModifiedJavaScriptSourceGenerator;
import dshell.lang.ModifiedTypeInfer;

import zen.ast.ZenNode;
import zen.ast.ZenStringNode;
import zen.deps.Field;
import zen.parser.ZenVisitor;

public class DShellCommandNode extends ZenNode {
	@Field public ArrayList<ZenNode> ArgumentList; // ["ls", "-la"]
	@Field public ZenNode PipedNextNode;

	public DShellCommandNode(ZenStringNode Node) {
		super();
		this.ArgumentList = new ArrayList<ZenNode>();
		this.ArgumentList.add(this.SetChild(Node));
		this.PipedNextNode = null;
	}

	@Override public void Append(ZenNode Node) {
		this.ArgumentList.add(this.SetChild(Node));
	}

	public ZenNode AppendPipedNextNode(DShellCommandNode Node) {
		this.PipedNextNode = this.SetChild(Node);
		return this;
	}

	@Override public void Accept(ZenVisitor Visitor) {
		if(Visitor instanceof ModifiedJavaScriptSourceGenerator) {
			((ModifiedJavaScriptSourceGenerator)Visitor).VisitCommandNode(this);
		}
		else if(Visitor instanceof ModifiedTypeInfer) {
			((ModifiedTypeInfer)Visitor).VisitCommandNode(this);
		}
		else {
			throw new RuntimeException(Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}