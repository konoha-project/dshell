package dshell.internal.lang;

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
import libbun.lang.bun.BunTypeSafer;
import libbun.parser.classic.BToken;
import libbun.parser.classic.LibBunLogger;
import libbun.type.BGenericType;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.type.BVarType;
import dshell.internal.ast.CommandNode;
import dshell.internal.ast.DShellCatchNode;
import dshell.internal.ast.DShellForNode;
import dshell.internal.ast.DShellTryNode;
import dshell.internal.ast.DShellWrapperNode;
import dshell.internal.ast.InternalFuncCallNode;
import dshell.internal.ast.MatchRegexNode;
import dshell.internal.ast.sugar.DShellForeachNode;
import dshell.internal.jvm.JavaByteCodeGenerator;
import dshell.internal.lib.CommandArg;
import dshell.lang.Exception;

public class DShellTypeChecker extends BunTypeSafer implements DShellVisitor {

	public DShellTypeChecker(JavaByteCodeGenerator generator) {
		super(generator);
	}

	@Override
	public void visitCommandNode(CommandNode node) {
		BType contextType = this.GetContextType();
		if(!(node.ParentNode instanceof CommandNode)) {
			if(node.retType().IsStringType() && node.ParentNode instanceof DShellForeachNode) {
				contextType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.StringType);
			}
			else if(node.retType().IsStringType()) {
				contextType = BType.StringType;
			}
			else if(contextType.IsVarType() && node.ParentNode instanceof BunBlockNode) {
				contextType = BType.VoidType;
			}
			else if(contextType.IsVarType()) {
				contextType = BType.StringType;
			}
		}
		int size = node.getArgSize();
		for(int i = 0; i < size; i++) {
			BNode subNode = node.getArgAt(i);
			subNode = this.CheckType(subNode, ((JavaByteCodeGenerator) this.Generator).getTypeTable().GetBunType(CommandArg.class));
			node.setArgAt(i, subNode);
		}
		if(node.getPipedNextNode() != null) {
			node.setPipedNextNode((CommandNode) this.CheckType(node.getPipedNextNode(), contextType));
		}
		this.ReturnTypeNode(node, contextType);
	}

	@Override
	public void visitTryNode(DShellTryNode node) {
		this.CheckTypeAt(node, DShellTryNode._Try, BType.VoidType);
		int size = node.GetListSize();
		for(int i = 0; i < size; i++) {
			BNode catchNode = node.GetListAt(i);
			catchNode = this.CheckType(catchNode, BType.VoidType);
			node.SetListAt(i, catchNode);
		}
		if(node.hasFinallyBlockNode()) {
			this.CheckTypeAt(node, DShellTryNode._Finally, BType.VoidType);
		}
		this.ReturnTypeNode(node, BType.VoidType);
	}

	@Override
	public void visitCatchNode(DShellCatchNode node) {
		node.setDefaultType(((JavaByteCodeGenerator)this.Generator).getTypeTable());
		if(!this.checkTypeRequirement(node.exceptionType())) {
			this.ReturnErrorNode(node, node.GetAstToken(DShellCatchNode._TypeInfo), "require Exception type");
			return;
		}
		BunBlockNode blockNode = node.blockNode();
		if(!(node.exceptionType() instanceof BVarType)) {
			node.setExceptionType(this.VarScope.NewVarType(node.exceptionType(), node.exceptionName(), node.SourceToken));
			blockNode.GetBlockGamma().SetSymbol(node.exceptionName(), node.toLetVarNode());
		}
		this.VisitBlockNode(blockNode);
		if(blockNode.GetListSize() == 0) {
			LibBunLogger._LogWarning(node.SourceToken, "unused variable: " + node.exceptionName());
		}
		this.ReturnTypeNode(node, BType.VoidType);
	}

	private boolean checkTypeRequirement(BType exceptionType) {
		Class<?> javaClass = ((JavaByteCodeGenerator)this.Generator).getJavaClass(exceptionType);
		while(javaClass != null) {
			if(javaClass.equals(Exception.class)) {
				return true;
			}
			javaClass = javaClass.getSuperclass();
		}
		return false;
	}

	@Override public void VisitThrowNode(BunThrowNode node) {
		BunFunctionNode funcNode = this.findParentFuncNode(node);
		if(funcNode != null && funcNode == node.GetDefiningFunctionNode()) {
			this.CurrentFunctionNode.SetReturnType(BType.VoidType);
		}
		this.CheckTypeAt(node, BunThrowNode._Expr, BType.VarType);
		if(!this.checkTypeRequirement(node.ExprNode().Type)) {
			this.ReturnErrorNode(node, node.GetAstToken(BunThrowNode._Expr), "require Exception type");
			return;
		}
		this.ReturnTypeNode(node, BType.VoidType);
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

	@Override public void VisitInstanceOfNode(BunInstanceOfNode node) {
		this.CheckTypeAt(node, BinaryOperatorNode._Left, BType.VarType);
		this.ReturnTypeNode(node, BType.BooleanType);
	}

	@Override public void VisitSyntaxSugarNode(SyntaxSugarNode node) {
		if(node instanceof BunContinueNode) {
			this.visitContinueNode((BunContinueNode) node);
		}
		else {
			super.VisitSyntaxSugarNode(node);
		}
	}

	@Override
	public void visitContinueNode(BunContinueNode node) {
		BNode currentNode = node;
		boolean foundWhile = false;
		while(currentNode != null) {
			if(currentNode instanceof BunWhileNode || currentNode instanceof DShellForNode) {
				foundWhile = true;
				break;
			}
			currentNode = currentNode.ParentNode;
		}
		if(!foundWhile) {
			this.ReturnErrorNode(node, node.SourceToken, "only available inside loop statement");
			return;
		}
		this.ReturnTypeNode(node, BType.VoidType);
	}

	@Override
	public void visitForNode(DShellForNode node) {	//FIXME
		node.prepareTypeCheck();
		if(node.hasDeclNode()) {
			this.VisitVarDeclNode(node.blockNode().GetBlockGamma(), node.toVarDeclNode());
		}
		this.CheckTypeAt(node, DShellForNode._Block, BType.VoidType);
		this.CheckTypeAt(node, DShellForNode._Cond, BType.BooleanType);
		if(node.hasNextNode()) {
			this.CheckTypeAt(node, DShellForNode._Next, BType.VoidType);
		}
		if(node.blockNode().GetListSize() == 0) {
			LibBunLogger._LogWarning(node.SourceToken, "unused variable: " + node.toVarDeclNode().GetGivenName());
		}
		this.ReturnTypeNode(node, BType.VoidType);
	}

	@Override
	public void visitWrapperNode(DShellWrapperNode node) {
		node.Type = BType.VoidType;
		BNode targetNode = node.getTargetNode();
		targetNode = this.CheckType(targetNode, BType.VarType);
		targetNode.Type = BType.VoidType;
		node.setTargetNode(targetNode);
		this.ReturnNode(node);
	}

	@Override
	public void visitMatchRegexNode(MatchRegexNode node) {
		this.CheckTypeAt(node, BinaryOperatorNode._Left, BType.StringType);
		this.CheckTypeAt(node, BinaryOperatorNode._Right, BType.StringType);
		this.ReturnBinaryTypeNode(node, BType.BooleanType);
	}

	public BunFunctionNode visitTopLevelStatementNode(BNode node) {
		BNode parentNode = node.ParentNode;
		BToken sourceToken = node.SourceToken;
		String funcName = this.Generator.NameUniqueSymbol("topLevel");
		BunFunctionNode funcNode = new BunFunctionNode(parentNode);
		funcNode.Type = BType.VoidType;
		funcNode.GivenName = funcName;
		funcNode.SourceToken = sourceToken;
		BunBlockNode blockNode = new BunBlockNode(parentNode, null);
		funcNode.SetNode(BunFunctionNode._Block, blockNode);
		node.ParentNode = blockNode;
		this.CurrentFunctionNode = funcNode;
		node = this.CheckType(node, BType.VarType);
		this.CurrentFunctionNode = null;
		BunReturnNode returnNode = new BunReturnNode(parentNode);
		if(node.Type.IsVoidType()) {
			blockNode.Append(node);
		}
		else {
			returnNode.SetNode(BunReturnNode._Expr, node);
		}
		blockNode.Append(returnNode);
		funcNode.SetReturnType(node.Type);
		return funcNode;
	}

	@Override public void VisitNullNode(BunNullNode node) {
		BType type = this.GetContextType();
		if(type.IsIntType() || type.IsBooleanType() || type.IsFloatType() || type.IsVoidType()) {
			this.ReturnErrorNode(node, node.SourceToken, "null is not " + type + " type");
			return;
		}
		else if(type.IsVarType()) {
			this.ReturnErrorNode(node, node.SourceToken, "untyped null value");
			return;
		}
		this.ReturnTypeNode(node, type);
	}

	@Override
	public void visitInternalFuncCallNode(InternalFuncCallNode node) {
		this.ReturnTypeNode(node, node.getReturnType());
	}
}
