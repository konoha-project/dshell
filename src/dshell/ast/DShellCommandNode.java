package dshell.ast;

import java.util.ArrayList;

import dshell.codegen.javascript.ModifiedJavaScriptSourceGenerator;
import dshell.lang.ModifiedDynamicTypeChecker;

import zen.ast.ZenIntNode;
import zen.ast.ZenNode;
import zen.ast.ZenStringNode;
import zen.deps.Field;
import zen.lang.ZenType;
import zen.parser.ZenToken;
import zen.parser.ZenVisitor;

public class DShellCommandNode extends ZenNode {
	@Field public ArrayList<ZenNode> ArgumentList; // ["ls", "-la"]
	@Field public ZenNode OptionNode;
	@Field public ZenNode PipedNextNode;

	public DShellCommandNode(ZenType Type, ZenStringNode Node) {
		super();
		this.Type = Type;
		this.ArgumentList = new ArrayList<ZenNode>();
		this.ArgumentList.add(this.SetChild(Node));
		this.OptionNode = new ZenIntNode(new ZenToken(0, "0", 0), 0);
		this.PipedNextNode = null;
	}

	@Override public void Append(ZenNode Node) {
		this.ArgumentList.add(this.SetChild(Node));
	}

	public void AppendOption(ZenIntNode Node) {
		this.OptionNode = this.SetChild(Node);
	}

	public void AppendPipedNextNode(DShellCommandNode Node) {
		this.PipedNextNode = this.SetChild(Node);
	}

	@Override public void Accept(ZenVisitor Visitor) {
		if(Visitor instanceof ModifiedJavaScriptSourceGenerator) {
			((ModifiedJavaScriptSourceGenerator)Visitor).VisitCommandNode(this);
		}
		else if(Visitor instanceof ModifiedDynamicTypeChecker) {
			((ModifiedDynamicTypeChecker)Visitor).VisitCommandNode(this);
		}
		else {
			throw new RuntimeException(Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}