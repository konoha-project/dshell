package dshell.lang;

import zen.ast.ZCatchNode;
import zen.ast.ZNode;
import zen.deps.NativeTypeTable;
import zen.lang.ZenTypeSafer;
import zen.parser.ZGenerator;
import zen.type.ZType;
import dshell.ast.DShellCommandNode;
import dshell.ast.DShellTryNode;
import dshell.lib.Task;

public class ModifiedTypeSafer extends ZenTypeSafer {
	public ModifiedTypeSafer(ZGenerator Generator) {
		super(Generator);
	}

	public void VisitCommandNode(DShellCommandNode Node) {
		ZType ContextType = this.GetContextType();
		if(!ContextType.IsBooleanType() && !ContextType.IsIntType() && !ContextType.IsStringType() && !ContextType.IsVoidType()) {
			ContextType = NativeTypeTable.GetZenType(Task.class);
		}
		int size = Node.GetListSize();
		for(int i = 0; i < size; i++) {
			ZNode SubNode = Node.GetListAt(i);
			SubNode = this.CheckType(SubNode, ZType.StringType);
			Node.SetListAt(i, SubNode);
		}
		if(Node.PipedNextNode != null) {
			Node.PipedNextNode = this.CheckType(Node.PipedNextNode, ContextType);
		}
		this.TypedNode(Node, ContextType);
	}

	public void VisitTryNode(DShellTryNode Node) {
		Node.AST[DShellTryNode.Try] = this.CheckType(Node.AST[DShellTryNode.Try], ZType.VoidType);
		int size = Node.GetListSize();
		for(int i = 0; i < size; i++) {
			ZNode CatchNode = Node.GetListAt(i);
			CatchNode = this.CheckType(CatchNode, ZType.VoidType);
			Node.SetListAt(i, CatchNode);
		}
		if(Node.AST[DShellTryNode.Finally] != null) {
			Node.AST[DShellTryNode.Finally] = this.CheckType(Node.AST[DShellTryNode.Finally], ZType.VoidType);
		}
		this.TypedNode(Node, ZType.VoidType);
	}

	@Override public void VisitCatchNode(ZCatchNode Node) {	//FIXME
		Node.AST[ZCatchNode.Block] = this.CheckType(Node.AST[ZCatchNode.Block], ZType.VoidType);
		this.TypedNode(Node, ZType.VoidType);
	}
}
