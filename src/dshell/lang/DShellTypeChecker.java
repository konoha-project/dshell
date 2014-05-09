package dshell.lang;

import libbun.ast.BunBlockNode;
import libbun.ast.BNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.binary.BunInstanceOfNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.ast.literal.BunNullNode;
import libbun.ast.statement.BunReturnNode;
import libbun.ast.statement.BunThrowNode;
import libbun.ast.statement.BunWhileNode;
import libbun.ast.sugar.BunContinueNode;
import libbun.encode.jvm.JavaTypeTable;
import libbun.encode.jvm.DShellByteCodeGenerator;
import libbun.lang.bun.BunTypeSafer;
import libbun.parser.classic.BToken;
import libbun.parser.classic.LibBunLogger;
import libbun.type.BGenericType;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.type.BVarType;
import dshell.ast.CommandNode;
import dshell.ast.DShellCatchNode;
import dshell.ast.DShellForNode;
import dshell.ast.DShellTryNode;
import dshell.ast.DShellWrapperNode;
import dshell.ast.InternalFuncCallNode;
import dshell.ast.MatchRegexNode;
import dshell.ast.sugar.DShellForeachNode;
import dshell.exception.Exception;
import dshell.lib.CommandArg;

public class DShellTypeChecker extends BunTypeSafer implements DShellVisitor {

	public DShellTypeChecker(DShellByteCodeGenerator Generator) {
		super(Generator);
	}

	@Override
	public void VisitCommandNode(CommandNode Node) {
		BType ContextType = this.GetContextType();
		if(!(Node.ParentNode instanceof CommandNode)) {
			if(Node.RetType().IsStringType() && Node.ParentNode instanceof DShellForeachNode) {
				ContextType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.StringType);
			}
			else if(Node.RetType().IsStringType()) {
				ContextType = BType.StringType;
			}
			else if(ContextType.IsVarType() && Node.ParentNode instanceof BunBlockNode) {
				ContextType = BType.VoidType;
			}
			else if(ContextType.IsVarType()) {
				ContextType = BType.StringType;
			}
		}
		int size = Node.GetArgSize();
		for(int i = 0; i < size; i++) {
			BNode SubNode = Node.GetArgAt(i);
			SubNode = this.CheckType(SubNode, JavaTypeTable.GetBunType(CommandArg.class));
			Node.SetArgAt(i, SubNode);
		}
		if(Node.PipedNextNode != null) {
			Node.PipedNextNode = (CommandNode) this.CheckType(Node.PipedNextNode, ContextType);
		}
		this.ReturnTypeNode(Node, ContextType);
	}

	@Override
	public void VisitTryNode(DShellTryNode Node) {
		this.CheckTypeAt(Node, DShellTryNode._Try, BType.VoidType);
		int size = Node.GetListSize();
		for(int i = 0; i < size; i++) {
			BNode CatchNode = Node.GetListAt(i);
			CatchNode = this.CheckType(CatchNode, BType.VoidType);
			Node.SetListAt(i, CatchNode);
		}
		if(Node.HasFinallyBlockNode()) {
			this.CheckTypeAt(Node, DShellTryNode._Finally, BType.VoidType);
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override
	public void VisitCatchNode(DShellCatchNode Node) {
		if(!this.CheckTypeRequirement(Node.ExceptionType())) {
			this.ReturnErrorNode(Node, Node.GetAstToken(DShellCatchNode._TypeInfo), "require Exception type");
			return;
		}
		BunBlockNode BlockNode = Node.BlockNode();
		if(!(Node.ExceptionType() instanceof BVarType)) {
			Node.SetExceptionType(this.VarScope.NewVarType(Node.ExceptionType(), Node.ExceptionName(), Node.SourceToken));
			BlockNode.GetBlockGamma().SetSymbol(Node.ExceptionName(), Node.ToLetVarNode());
		}
		this.VisitBlockNode(BlockNode);
		if(BlockNode.GetListSize() == 0) {
			LibBunLogger._LogWarning(Node.SourceToken, "unused variable: " + Node.ExceptionName());
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	private boolean CheckTypeRequirement(BType ExceptionType) {
		Class<?> JavaClass = ((DShellByteCodeGenerator)this.Generator).GetJavaClass(ExceptionType);
		while(JavaClass != null) {
			if(JavaClass.equals(Exception.class)) {
				return true;
			}
			JavaClass = JavaClass.getSuperclass();
		}
		return false;
	}

	@Override public void VisitThrowNode(BunThrowNode Node) {
		BunFunctionNode FuncNode = this.findParentFuncNode(Node);
		if(FuncNode != null && FuncNode == Node.GetDefiningFunctionNode()) {
			this.CurrentFunctionNode.SetReturnType(BType.VoidType);
		}
		this.CheckTypeAt(Node, BunThrowNode._Expr, BType.VarType);
		if(!this.CheckTypeRequirement(Node.ExprNode().Type)) {
			this.ReturnErrorNode(Node, Node.GetAstToken(BunThrowNode._Expr), "require Exception type");
			return;
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	private BunFunctionNode findParentFuncNode(BNode node) {
		if(node == null) {
			return null;
		}
		if(node instanceof BunBlockNode && !(node instanceof BunVarBlockNode)) {
			if(node.ParentNode != null && node.ParentNode instanceof BunFunctionNode) {
				return (BunFunctionNode) node.ParentNode;
			}
		}
		else {
			return this.findParentFuncNode(node.ParentNode);
		}
		return null;
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.VarType);
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	@Override public void VisitSyntaxSugarNode(SyntaxSugarNode Node) {
		if(Node instanceof BunContinueNode) {
			this.VisitContinueNode((BunContinueNode) Node);
		}
		else {
			super.VisitSyntaxSugarNode(Node);
		}
	}

	@Override
	public void VisitContinueNode(BunContinueNode Node) {
		BNode CurrentNode = Node;
		boolean FoundWhile = false;
		while(CurrentNode != null) {
			if(CurrentNode instanceof BunWhileNode || CurrentNode instanceof DShellForNode) {
				FoundWhile = true;
				break;
			}
			CurrentNode = CurrentNode.ParentNode;
		}
		if(!FoundWhile) {
			this.ReturnErrorNode(Node, Node.SourceToken, "only available inside loop statement");
			return;
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override
	public void VisitForNode(DShellForNode Node) {	//FIXME
		Node.PrepareTypeCheck();
		if(Node.HasDeclNode()) {
			this.VisitVarDeclNode(Node.BlockNode().GetBlockGamma(), Node.VarDeclNode());
		}
		this.CheckTypeAt(Node, DShellForNode._Block, BType.VoidType);
		this.CheckTypeAt(Node, DShellForNode._Cond, BType.BooleanType);
		if(Node.HasNextNode()) {
			this.CheckTypeAt(Node, DShellForNode._Next, BType.VoidType);
		}
		if(Node.BlockNode().GetListSize() == 0) {
			LibBunLogger._LogWarning(Node.SourceToken, "unused variable: " + Node.VarDeclNode().GetGivenName());
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override
	public void VisitWrapperNode(DShellWrapperNode Node) {
		Node.Type = BType.VoidType;
		BNode TargetNode = Node.getTargetNode();
		TargetNode = this.CheckType(TargetNode, BType.VarType);
		TargetNode.Type = BType.VoidType;
		Node.setTargetNode(TargetNode);
		this.ReturnNode(Node);
	}

	@Override
	public void VisitMatchRegexNode(MatchRegexNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.StringType);
		this.CheckTypeAt(Node, BinaryOperatorNode._Right, BType.StringType);
		this.ReturnBinaryTypeNode(Node, BType.BooleanType);
	}

	public BunFunctionNode VisitTopLevelStatementNode(BNode Node) {
		BNode ParentNode = Node.ParentNode;
		BToken SourceToken = Node.SourceToken;
		String FuncName = this.Generator.NameUniqueSymbol("topLevel");
		BunFunctionNode FuncNode = new BunFunctionNode(ParentNode);
		FuncNode.Type = BType.VoidType;
		FuncNode.GivenName = FuncName;
		FuncNode.SourceToken = SourceToken;
		BunBlockNode BlockNode = new BunBlockNode(ParentNode, null);
		FuncNode.SetNode(BunFunctionNode._Block, BlockNode);
		Node.ParentNode = BlockNode;
		this.CurrentFunctionNode = FuncNode;
		Node = this.CheckType(Node, BType.VarType);
		this.CurrentFunctionNode = null;
		BunReturnNode ReturnNode = new BunReturnNode(ParentNode);
		if(Node.Type.IsVoidType()) {
			BlockNode.Append(Node);
		}
		else {
			ReturnNode.SetNode(BunReturnNode._Expr, Node);
		}
		BlockNode.Append(ReturnNode);
		FuncNode.SetReturnType(Node.Type);
		return FuncNode;
	}

	@Override public void VisitNullNode(BunNullNode Node) {
		BType Type = this.GetContextType();
		if(Type.IsIntType() || Type.IsBooleanType() || Type.IsFloatType() || Type.IsVoidType()) {
			this.ReturnErrorNode(Node, Node.SourceToken, "null is not " + Type + " type");
			return;
		}
		else if(Type.IsVarType()) {
			this.ReturnErrorNode(Node, Node.SourceToken, "untyped null value");
			return;
		}
		this.ReturnTypeNode(Node, Type);
	}

	@Override
	public void VisitInternalFuncCallNode(InternalFuncCallNode Node) {
		this.ReturnTypeNode(Node, Node.getReturnType());
	}
}
