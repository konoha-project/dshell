package dshell.lang;

import zen.ast.ZBlockNode;
import zen.ast.ZNode;
import zen.ast.ZSugarNode;
import zen.ast.ZThrowNode;
import zen.ast.ZVarNode;
import zen.ast.ZWhileNode;
import zen.ast.sugar.ZContinueNode;
import zen.codegen.jvm.JavaTypeTable;
import zen.codegen.jvm.ModifiedAsmGenerator;
import zen.lang.zen.ZenTypeSafer;
import zen.parser.ZLogger;
import zen.type.ZGenericType;
import zen.type.ZType;
import zen.type.ZTypePool;
import zen.type.ZVarType;
import dshell.ast.DShellCatchNode;
import dshell.ast.DShellForNode;
import dshell.ast.DShellTryNode;
import dshell.ast.sugar.DShellCommandNode;
import dshell.ast.sugar.DShellForeachNode;
import dshell.lib.CommandArg;

public class ModifiedTypeSafer extends ZenTypeSafer implements DShellVisitor {
	public ModifiedTypeSafer(ModifiedAsmGenerator Generator) {
		super(Generator);
	}

	@Override
	public void VisitCommandNode(DShellCommandNode Node) {	//FIXME
		ZType ContextType = this.GetContextType();
		if(!(Node.ParentNode instanceof DShellCommandNode)) {
			if(Node.RetType().IsStringType() && Node.ParentNode instanceof DShellForeachNode) {
				ContextType = ZTypePool._GetGenericType1(ZGenericType._ArrayType, ZType.StringType);
			}
			else if(Node.RetType().IsStringType()) {
				ContextType = ZType.StringType;
			}
			else if(ContextType.IsVarType() && Node.ParentNode instanceof ZBlockNode) {
				ContextType = ZType.VoidType;
			}
			else if(ContextType.IsVarType()) {
				ContextType = ZType.StringType;
			}
		}
		int size = Node.GetArgSize();
		for(int i = 0; i < size; i++) {
			ZNode SubNode = Node.GetArgAt(i);
			SubNode = this.CheckType(SubNode, JavaTypeTable.GetZenType(CommandArg.class));
			Node.SetArgAt(i, SubNode);
		}
		if(Node.PipedNextNode != null) {
			Node.PipedNextNode = this.CheckType(Node.PipedNextNode, ContextType);
		}
		this.ReturnTypeNode(Node, ContextType);
	}

	@Override
	public void VisitTryNode(DShellTryNode Node) {
		this.CheckTypeAt(Node, DShellTryNode._Try, ZType.VoidType);
		int size = Node.GetListSize();
		for(int i = 0; i < size; i++) {
			ZNode CatchNode = Node.GetListAt(i);
			CatchNode = this.CheckType(CatchNode, ZType.VoidType);
			Node.SetListAt(i, CatchNode);
		}
		if(Node.HasFinallyBlockNode()) {
			this.CheckTypeAt(Node, DShellTryNode._Finally, ZType.VoidType);
		}
		this.ReturnTypeNode(Node, ZType.VoidType);
	}

	@Override
	public void VisitCatchNode(DShellCatchNode Node) {
		ZBlockNode BlockNode = Node.BlockNode();
		if(!(Node.ExceptionType() instanceof ZVarType)) {
			Node.SetExceptionType(this.VarScope.NewVarType(Node.ExceptionType(), Node.ExceptionName(), Node.SourceToken));
			BlockNode.GetBlockNameSpace().SetLocalVariable(this.CurrentFunctionNode, Node.ExceptionType(), Node.ExceptionName(), Node.SourceToken);
		}
		this.CheckTypeAt(Node, DShellCatchNode._Block, ZType.VoidType);
		if(BlockNode.GetListSize() == 0) {
			ZLogger._LogWarning(Node.SourceToken, "unused variable: " + Node.ExceptionName());
		}
		this.ReturnTypeNode(Node, ZType.VoidType);
	}

	@Override public void VisitThrowNode(ZThrowNode Node) {
		this.CheckTypeAt(Node, ZThrowNode._Expr, JavaTypeTable.GetZenType(Throwable.class));
		this.ReturnTypeNode(Node, ZType.VoidType);
	}

	@Override public void VisitSugarNode(ZSugarNode Node) {
		if(Node instanceof ZContinueNode) {
			this.VisitContinueNode((ZContinueNode) Node);
		}
		else if(Node instanceof DShellCommandNode) {
			this.VisitCommandNode((DShellCommandNode) Node);
		}
		else {
			super.VisitSugarNode(Node);
		}
	}

	@Override
	public void VisitContinueNode(ZContinueNode Node) {
		ZNode CurrentNode = Node;
		boolean foundWhile = false;
		while(CurrentNode != null) {
			if(CurrentNode instanceof ZWhileNode || CurrentNode instanceof DShellForNode) {
				foundWhile = true;
				break;
			}
			CurrentNode = CurrentNode.ParentNode;
		}
		if(!foundWhile) {
			this.ReturnErrorNode(Node, Node.SourceToken, "only available inside loop statement");
			return;
		}
		this.ReturnTypeNode(Node, ZType.VoidType);
	}

	@Override
	public void VisitForNode(DShellForNode Node) {
		Node.PrepareTypeCheck();
		if(Node.HasDeclNode()) {
			this.CheckTypeAt(Node, DShellForNode._InitValue, Node.DeclType());
			if(Node.DeclType().IsVarType()) {
				Node.SetDeclType(Node.GetAstType(ZVarNode._InitValue));
			}
			if(!Node.DeclType().IsVarType()) {
				Node.SetDeclType(this.VarScope.NewVarType(Node.DeclType(), Node.GetName(), Node.SourceToken));
				Node.BlockNode().GetBlockNameSpace().SetLocalVariable(this.CurrentFunctionNode, Node.DeclType(), Node.GetName(), Node.SourceToken);
			}
		}
		this.CheckTypeAt(Node, DShellForNode._Block, ZType.VoidType);
		this.CheckTypeAt(Node, DShellForNode._Cond, ZType.BooleanType);
		if(Node.HasNextNode()) {
			this.CheckTypeAt(Node, DShellForNode._Next, ZType.VoidType);
		}
		if(Node.BlockNode().GetListSize() == 0) {
			ZLogger._LogWarning(Node.SourceToken, "unused variable: " + Node.GetName());
		}
		this.ReturnTypeNode(Node, ZType.VoidType);
	}
}
