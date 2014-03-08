package dshell.lang;

import zen.ast.ZBlockNode;
import zen.ast.ZCatchNode;
import zen.ast.ZNode;
import zen.ast.ZThrowNode;
import zen.codegen.jvm.JavaTypeTable;
import zen.lang.ZenTypeSafer;
import zen.parser.ZGenerator;
import zen.parser.ZLogger;
import zen.type.ZType;
import zen.type.ZVarType;
import dshell.ast.DShellCatchNode;
import dshell.ast.DShellCommandNode;
import dshell.ast.DShellDummyNode;
import dshell.ast.DShellExportEnvNode;
import dshell.ast.DShellTryNode;
import dshell.lib.Task;

public class ModifiedTypeSafer extends ZenTypeSafer {
	public ModifiedTypeSafer(ZGenerator Generator) {
		super(Generator);
	}

	public void VisitCommandNode(DShellCommandNode Node) {
		ZType ContextType = this.GetContextType();
		if(!ContextType.IsBooleanType() && !ContextType.IsIntType() && !ContextType.IsStringType() && !ContextType.IsVoidType()) {
			ContextType = JavaTypeTable.GetZenType(Task.class);
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
		Node.AST[DShellTryNode._Try] = this.CheckType(Node.AST[DShellTryNode._Try], ZType.VoidType);
		int size = Node.GetListSize();
		for(int i = 0; i < size; i++) {
			ZNode CatchNode = Node.GetListAt(i);
			CatchNode = this.CheckType(CatchNode, ZType.VoidType);
			Node.SetListAt(i, CatchNode);
		}
		if(Node.AST[DShellTryNode._Finally] != null) {
			Node.AST[DShellTryNode._Finally] = this.CheckType(Node.AST[DShellTryNode._Finally], ZType.VoidType);
		}
		this.TypedNode(Node, ZType.VoidType);
	}

	public void VisitCatchNode(DShellCatchNode Node) {	//FIXME
		ZBlockNode BlockNode = (ZBlockNode)Node.AST[ZCatchNode._Block];
		if(BlockNode.GetListSize() == 0) {
			ZLogger._LogWarning(Node.SourceToken, "unused variable: " + Node.ExceptionName);
		}
		if(!(Node.ExceptionType instanceof ZVarType)) {
			Node.ExceptionType = this.VarScope.NewVarType(Node.ExceptionType, Node.ExceptionName, Node.SourceToken);
			BlockNode.NameSpace.SetLocalVariable(this.CurrentFunctionNode, Node.ExceptionType, Node.ExceptionName, Node.SourceToken);
		}
		Node.AST[ZCatchNode._Block] = this.CheckType(BlockNode, ZType.VoidType);
		this.TypedNode(Node, ZType.VoidType);
	}

	public void VisitDummyNode(DShellDummyNode Node) {	// do nothing
		this.Return(Node);
	}

	@Override public void VisitThrowNode(ZThrowNode Node) {
		this.CheckTypeAt(Node, ZThrowNode._Expr, JavaTypeTable.GetZenType(Throwable.class));
		this.TypedNode(Node, ZType.VoidType);
	}

	public void VisitExportEnvNode(DShellExportEnvNode Node) {
		this.CheckTypeAt(Node, DShellExportEnvNode._EXPORT, ZType.VarType);
		this.CheckTypeAt(Node, DShellExportEnvNode._LET, ZType.VoidType);
		this.TypedNode(Node, ZType.VoidType);
	}
}
