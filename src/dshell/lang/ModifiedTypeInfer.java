package dshell.lang;

import zen.ast.ZCatchNode;
import zen.ast.ZNode;
import zen.ast.ZTryNode;
import zen.deps.NativeTypeTable;
import zen.deps.Var;
import zen.lang.ZenTypeSafer;
import zen.parser.ZLogger;
import zen.parser.ZNameSpace;
import zen.type.ZType;
import dshell.ast.DShellCommandNode;
import dshell.lib.Task;

public class ModifiedTypeInfer extends ZenTypeSafer {
	public ModifiedTypeInfer(ZLogger Logger) {
		super(Logger);
	}

	public void VisitCommandNode(DShellCommandNode Node) {
		ZNameSpace NameSpace = this.GetNameSpace();
		ZType ContextType = this.GetContextType();
		if(!ContextType.IsBooleanType() && !ContextType.IsIntType() && !ContextType.IsStringType() && !ContextType.IsVoidType()) {
			ContextType = NativeTypeTable.GetZenType(Task.class);
		}
		int size = Node.ArgumentList.size();
		for(int i = 0; i < size; i++) {
			ZNode SubNode = Node.ArgumentList.get(i);
			SubNode = this.CheckType(SubNode, NameSpace, ZType.StringType);
			Node.ArgumentList.set(i, SubNode);
		}
		if(Node.PipedNextNode != null) {
			Node.PipedNextNode = this.CheckType(Node.PipedNextNode, NameSpace, ContextType);
		}
		this.TypedNode(Node, ContextType);
	}

	@Override public void VisitTryNode(ZTryNode Node) {
		@Var ZNameSpace NameSpace = this.GetNameSpace();
		Node.TryNode = this.CheckType(Node.TryNode, NameSpace, ZType.VoidType);
		if(Node.CatchNode != null) {
			Node.CatchNode = this.CheckType(Node.CatchNode, NameSpace, ZType.VoidType);
		}
		if(Node.FinallyNode != null) {
			Node.FinallyNode = this.CheckType(Node.FinallyNode, NameSpace, ZType.VoidType);
		}
		this.TypedNode(Node, ZType.VoidType);
	}

	@Override public void VisitCatchNode(ZCatchNode Node) {
		ZNameSpace NameSpace = this.GetNameSpace();
		Node.BodyNode = this.CheckType(Node.BodyNode, NameSpace, ZType.VoidType);
		this.TypedNode(Node, ZType.VoidType);
	}
}
