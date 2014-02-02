package dshell.lang;

import zen.ast.ZCatchNode;
import zen.ast.ZNode;
import zen.deps.NativeTypeTable;
import zen.lang.ZenTypeSafer;
import zen.parser.ZGenerator;
import zen.parser.ZLogger;
import zen.parser.ZNameSpace;
import zen.type.ZType;
import dshell.ast.DShellCommandNode;
import dshell.ast.DShellTryNode;
import dshell.lib.Task;

public class ModifiedTypeInfer extends ZenTypeSafer {
	public ModifiedTypeInfer(ZGenerator Generator) {
		super(Generator);
	}

	public void VisitCommandNode(DShellCommandNode Node) {
		ZType ContextType = this.GetContextType();
		if(!ContextType.IsBooleanType() && !ContextType.IsIntType() && !ContextType.IsStringType() && !ContextType.IsVoidType()) {
			ContextType = NativeTypeTable.GetZenType(Task.class);
		}
		int size = Node.ArgumentList.size();
		for(int i = 0; i < size; i++) {
			ZNode SubNode = Node.ArgumentList.get(i);
			SubNode = this.CheckType(SubNode, ZType.StringType);
			Node.ArgumentList.set(i, SubNode);
		}
		if(Node.PipedNextNode != null) {
			Node.PipedNextNode = this.CheckType(Node.PipedNextNode, ContextType);
		}
		this.TypedNode(Node, ContextType);
	}

	public void VisitTryNode(DShellTryNode Node) {
		Node.TryNode = this.CheckType(Node.TryNode, ZType.VoidType);
		int size = Node.CatchNodeList.size();
		for(int i = 0; i < size; i++) {
			ZNode CatchNode = Node.CatchNodeList.get(i);
			CatchNode = this.CheckType(CatchNode, ZType.VoidType);
			Node.CatchNodeList.set(i, CatchNode);
		}
		if(Node.FinallyNode != null) {
			Node.FinallyNode = this.CheckType(Node.FinallyNode, ZType.VoidType);
		}
		this.TypedNode(Node, ZType.VoidType);
	}

	@Override public void VisitCatchNode(ZCatchNode Node) {
		Node.BodyNode = this.CheckType(Node.BodyNode, ZType.VoidType);
		this.TypedNode(Node, ZType.VoidType);
	}
}
