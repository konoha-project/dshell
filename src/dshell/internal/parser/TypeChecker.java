package dshell.internal.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import dshell.internal.parser.CalleeHandle.ConstructorHandle;
import dshell.internal.parser.CalleeHandle.FieldHandle;
import dshell.internal.parser.CalleeHandle.MethodHandle;
import dshell.internal.parser.CalleeHandle.OperatorHandle;
import dshell.internal.parser.CalleeHandle.StaticFieldHandle;
import dshell.internal.parser.Node.ArrayNode;
import dshell.internal.parser.Node.AssertNode;
import dshell.internal.parser.Node.AssignNode;
import dshell.internal.parser.Node.BlockEndNode;
import dshell.internal.parser.Node.BlockNode;
import dshell.internal.parser.Node.BooleanValueNode;
import dshell.internal.parser.Node.BreakNode;
import dshell.internal.parser.Node.CastNode;
import dshell.internal.parser.Node.CatchNode;
import dshell.internal.parser.Node.ClassNode;
import dshell.internal.parser.Node.CondOpNode;
import dshell.internal.parser.Node.ConstructorCallNode;
import dshell.internal.parser.Node.ConstructorNode;
import dshell.internal.parser.Node.ContinueNode;
import dshell.internal.parser.Node.ElementGetterNode;
import dshell.internal.parser.Node.EmptyBlockNode;
import dshell.internal.parser.Node.EmptyNode;
import dshell.internal.parser.Node.ExportEnvNode;
import dshell.internal.parser.Node.ExprNode;
import dshell.internal.parser.Node.FieldGetterNode;
import dshell.internal.parser.Node.FloatValueNode;
import dshell.internal.parser.Node.ForInNode;
import dshell.internal.parser.Node.ForNode;
import dshell.internal.parser.Node.FunctionNode;
import dshell.internal.parser.Node.IfNode;
import dshell.internal.parser.Node.ImportEnvNode;
import dshell.internal.parser.Node.InstanceofNode;
import dshell.internal.parser.Node.IntValueNode;
import dshell.internal.parser.Node.InvokeNode;
import dshell.internal.parser.Node.LoopNode;
import dshell.internal.parser.Node.MapNode;
import dshell.internal.parser.Node.NullNode;
import dshell.internal.parser.Node.OperatorCallNode;
import dshell.internal.parser.Node.ReturnNode;
import dshell.internal.parser.Node.RootNode;
import dshell.internal.parser.Node.StringValueNode;
import dshell.internal.parser.Node.SuffixIncrementNode;
import dshell.internal.parser.Node.SymbolNode;
import dshell.internal.parser.Node.ThrowNode;
import dshell.internal.parser.Node.TryNode;
import dshell.internal.parser.Node.VarDeclNode;
import dshell.internal.parser.Node.WhileNode;
import dshell.internal.parser.SymbolTable.SymbolEntry;
import dshell.internal.parser.TypePool.ClassType;
import dshell.internal.parser.TypePool.FuncHolderType;
import dshell.internal.parser.TypePool.FunctionType;
import dshell.internal.parser.TypePool.PrimitiveType;
import dshell.internal.parser.TypePool.RootClassType;
import dshell.internal.parser.TypePool.Type;
import dshell.internal.parser.TypePool.UnresolvedType;
import dshell.internal.parser.TypePool.VoidType;

public class TypeChecker implements NodeVisitor<Node>{
	private final TypePool typePool;
	private final SymbolTable symbolTable;
	private final AbstractOperatorTable opTable;

	public TypeChecker(TypePool typePool) {
		this.typePool = typePool;
		this.symbolTable = new SymbolTable();
		this.opTable = new OperatorTable(this.typePool);
	}

	/**
	 * check node type.
	 * @param targetNode
	 * @return
	 * - typed this node.
	 */
	private Node checkType(Node targetNode) {
		return this.checkType(null, targetNode);
	}

	/**
	 * check node type
	 * @param requiredType
	 * @param targetNode
	 * @return
	 * - typed this node.
	 * if requiredType is not equivalent to node type, throw exception.
	 * if requiredType is null, do not try matching node type.
	 */
	private Node checkType(Type requiredType, Node targetNode) {
		/**
		 * if target node is statement, always check type.
		 */
		if(!(targetNode instanceof ExprNode)) {
			return targetNode.accept(this);
		}

		/**
		 * if target node is expr node and unresolved type, 
		 * try type check.
		 */
		ExprNode exprNode = (ExprNode) targetNode;
		if(exprNode.getType() instanceof UnresolvedType) {
			exprNode = (ExprNode) exprNode.accept(this);
		}

		/**
		 * after type checking, if type is still unresolved type, 
		 * throw exception.
		 */
		Type type = exprNode.getType();
		if(type instanceof UnresolvedType) {
			this.throwAndReportTypeError(exprNode, "having unresolved type");
		}

		/**
		 * do not try type matching.
		 */
		if(requiredType == null) {
			return exprNode;
		}

		/**
		 * try type matching.
		 */
		if(requiredType.isAssignableFrom(type)) {
			return exprNode;
		}
		if((requiredType instanceof RootClassType) && (type instanceof PrimitiveType)) {	// boxing
			return new CastNode(requiredType, exprNode);
		}
		this.throwAndReportTypeError(exprNode, "require " + requiredType + ", but is " + type);
		return null;
	}

	private ClassType checkAndGetClassType(ExprNode recvNode) {
		this.checkType(this.typePool.objectType, recvNode);
		Type resolvedType = recvNode.getType();
		if(!(resolvedType instanceof ClassType)) {
			this.throwAndReportTypeError(recvNode, "require class type, but is " + resolvedType);
			return null;
		}
		return (ClassType) resolvedType;
	}

	/**
	 * check node type and try type matching.
	 * used for FuncCallNode, ConstructorCallNode and MethodCallNode
	 * @param requireTypeList
	 * @param paramNodeList
	 * - after type matching, paramNode structure may be changed due to primitive boxing.
	 * @param index
	 */
	private void checkParamTypeAt(List<Type> requireTypeList, List<ExprNode> paramNodeList, int index) {
		ExprNode checkedNode = (ExprNode) this.checkType(requireTypeList.get(index), paramNodeList.get(index));
		paramNodeList.set(index, checkedNode);
	}

	/**throwAndReportTypeError
	 * create new symbol table and check type each node within block.
	 * after type checking, remove current symbol table.
	 * @param blockNode
	 */
	private void checkTypeWithNewBlockScope(BlockNode blockNode) {
		this.symbolTable.createAndPushNewTable();
		this.checkTypeWithCurrentBlockScope(blockNode);
		this.symbolTable.popCurrentTable();
	}

	/**
	 * check type each node within block in current block scope.
	 * @param blockNode
	 */
	private void checkTypeWithCurrentBlockScope(BlockNode blockNode) {
		blockNode.accept(this);
	}

	private void addEntryAndThrowIfDefined(Node node, String symbolName, Type type, boolean isReadOnly) {
		if(!this.symbolTable.addEntry(symbolName, type, isReadOnly)) {
			this.throwAndReportTypeError(node, "already defined symbol: " + symbolName);
		}
	}

	/**
	 * check node inside loop.
	 * if node is out of loop, throw exception
	 * @param node
	 * - break node or continue node
	 */
	private void checkAndThrowIfOutOfLoop(Node node) {
		Node parentNode = node.getParentNode();
		while(true) {
			if(parentNode == null) {
				break;
			}
			if((parentNode instanceof FunctionNode) || (parentNode instanceof RootNode)) {
				break;
			}
			if(parentNode instanceof LoopNode) {
				return;
			}
			parentNode = parentNode.getParentNode();
		}
		this.throwAndReportTypeError(node, "only available inside loop statement");
	}

	private boolean findBlockEnd(BlockNode blockNode) {
		if(blockNode instanceof EmptyBlockNode) {
			return false;
		}
		List<Node> nodeList = blockNode.getNodeList();
		int endIndex = nodeList.size() - 1;
		if(endIndex < 0) {
			return false;
		}
		Node endNode = nodeList.get(endIndex);
		if(endNode instanceof BlockEndNode) {
			return true;
		}
		if(endNode instanceof IfNode) {
			IfNode ifNode = (IfNode) endNode;
			return this.findBlockEnd(ifNode.getThenBlockNode()) && this.findBlockEnd(ifNode.getElseBlockNode());
		}
		return false;
	}

	/**
	 * check block end (return, throw) existence in function block.
	 * @param blockNode
	 * - function block.
	 * @param returnType
	 * - function return type.
	 */
	private void checkBlockEndExistence(BlockNode blockNode, Type returnType) {
		int endIndex = blockNode.getNodeList().size() - 1;
		Node endNode = blockNode.getNodeList().get(endIndex);
		if((returnType instanceof VoidType) && !(endNode instanceof BlockEndNode)) {
			blockNode.getNodeList().add(new ReturnNode(null, new EmptyNode()));
			return;
		}
		if(!this.findBlockEnd(blockNode)) {
			this.throwAndReportTypeError(endNode, "not found return statement");
		}
	}

	@Override
	public Node visit(IntValueNode node) {
		node.setType(this.typePool.intType);
		return node;
	}

	@Override
	public Node visit(FloatValueNode node) {
		node.setType(this.typePool.floatType);
		return node;
	}

	@Override
	public Node visit(BooleanValueNode node) {
		node.setType(this.typePool.booleanType);
		return node;
	}

	@Override
	public Node visit(StringValueNode node) {
		node.setType(this.typePool.stringType);
		return node;
	}

	@Override
	public Node visit(NullNode node) {	//TODO: future may be used.
		return node;
	}

	@Override
	public Node visit(ArrayNode node) {
		int elementSize = node.getNodeList().size();
		assert elementSize != 0;
		ExprNode firstElementNode = node.getNodeList().get(0);
		this.checkType(firstElementNode);
		Type elementType = firstElementNode.getType();
		for(int i = 1; i < elementSize; i++) {
			this.checkType(elementType, node.getNodeList().get(i));
		}
		String baseArrayName = this.typePool.baseArrayType.getTypeName();
		node.setType(this.typePool.createAndGetGenericTypeIfUndefined(baseArrayName, new Type[]{elementType}));
		return node;
	}

	@Override
	public Node visit(MapNode node) {
		int entrySize = node.getKeyList().size();
		assert entrySize != 0;
		ExprNode firstValueNode = node.getValueList().get(0);
		this.checkType(firstValueNode);
		Type valueType = firstValueNode.getType();
		for(int i = 0; i < entrySize; i++) {
			this.checkType(this.typePool.stringType, node.getKeyList().get(i));
			this.checkType(valueType, node.getValueList().get(i));
		}
		String baseMapName = this.typePool.baseMapType.getTypeName();
		node.setType(this.typePool.createAndGetGenericTypeIfUndefined(baseMapName, new Type[]{valueType}));
		return node;
	}

	@Override
	public Node visit(SymbolNode node) {
		SymbolEntry entry = this.symbolTable.getEntry(node.getSymbolName());
		if(entry == null) {
			this.throwAndReportTypeError(node, "undefined symbol: " + node.getSymbolName());
		}
		node.setReadOnly(entry.isReadOnly());
		Type type = entry.getType();
		if(type instanceof FuncHolderType) {	// function field
			StaticFieldHandle handle = ((FuncHolderType)type).getFieldHandle();
			node.setHandle(handle);
			type = handle.getFieldType();
		}
		node.setType(type);
		return node;
	}

	@Override
	public Node visit(ElementGetterNode node) {	//TODO: method handle property
		this.throwAndReportTypeError(node, "unimplemtned type checker: " + node.getClass().getSimpleName());
		return null;
	}

	@Override
	public Node visit(FieldGetterNode node) {
		ClassType recvType = this.checkAndGetClassType(node.getRecvNode());
		FieldHandle handle = recvType.lookupFieldHandle(node.getFieldName());
		if(handle == null) {
			this.throwAndReportTypeError(node, "undefined field: " + node.getFieldName());
			return null;
		}
		node.setHandle(handle);
		node.setType(handle.getFieldType());
		return node;
	}

	@Override
	public Node visit(CastNode node) { //TODO:
		this.checkType(node.getExprNode());
		Type targetType = node.getTypeSymbol().toType(this.typePool);
		node.setTargteType(targetType);
		node.setType(targetType);
		return node;
	}

	@Override
	public Node visit(InstanceofNode node) {	//TODO:
		this.checkType(node.getExprNode());
		node.setTargetType(node.getTypeSymbol().toType(this.typePool));
		node.setType(this.typePool.booleanType);
		return node;
	}

	@Override
	public Node visit(SuffixIncrementNode node) {
		String op = node.getOperator();
		if(!op.equals("++") && !op.equals("--")) {
			this.throwAndReportTypeError(node, "undefined suffix operator: " + op);
		}
		this.checkType(node.getSymbolNode());
		if(node.getSymbolNode().isReadOnly) {
			this.throwAndReportTypeError(node.getSymbolNode(), "read only variable: " + node.getSymbolNode().getSymbolName());
		}
		Type exprType = node.getSymbolNode().getType();
		if(!this.typePool.intType.isAssignableFrom(exprType) && !this.typePool.floatType.isAssignableFrom(exprType)) {
			this.throwAndReportTypeError(node, "undefined suffix operator: " + exprType + " " + op);
		}
		node.setType(exprType);
		return node;
	}

	@Override
	public Node visit(OperatorCallNode node) {
		int size = node.getNodeList().size();
		assert (size > 0 && size < 3);
		List<ExprNode> paramNodeList = node.getNodeList();
		for(Node paramNode : paramNodeList) {
			this.checkType(paramNode);
		}
		OperatorHandle handle;
		if(size == 1) {
			Type rightType = paramNodeList.get(0).getType();
			handle = this.opTable.getOperatorHandle(node.getFuncName(), rightType);
			if(handle == null) {
				this.throwAndReportTypeError(node, "undefined operator: " + node.getFuncName() + " " + rightType);
			}
		} else {
			Type leftType = paramNodeList.get(0).getType();
			Type rightType = paramNodeList.get(1).getType();
			handle = this.opTable.getOperatorHandle(node.getFuncName(), leftType, rightType);
			if(handle == null) {
				this.throwAndReportTypeError(node, "undefined operator: " + leftType + " " + node.getFuncName() + " " + rightType);
			}
		}
		node.setHandle(handle);
		node.setType(handle.getReturnType());
		return node;
	}

	/**
	 * treat invoke node as function call
	 * @param node
	 * @return
	 */
	protected Node checkTypeAsFuncCall(InvokeNode node) {
		node.setAsFuncCallNode();
		MethodHandle handle = this.lookupFuncHandle(node.getRecvNode());

		// check param type
		int paramSize = handle.getParamTypeList().size();
		if(handle.getParamTypeList().size() != node.getArgList().size()) {
			this.throwAndReportTypeError(node, "not match parameter size: function is " + paramSize + ", but params size is " + node.getArgList().size());
		}
		for(int i = 0; i < paramSize; i++) {
			this.checkParamTypeAt(handle.getParamTypeList(), node.getArgList(), i);
		}
		node.setHandle(handle);
		node.setType(handle.getReturnType());
		return node;
	}

	protected MethodHandle lookupFuncHandle(ExprNode recvNode) {
		// look up static function
		if(recvNode instanceof SymbolNode) {
			String funcName = ((SymbolNode) recvNode).getSymbolName();
			SymbolEntry entry = this.symbolTable.getEntry(funcName);
			if(entry != null && entry.getType() instanceof FuncHolderType) {
				return ((FuncHolderType) entry.getType()).getFuncHanle();
			}
		}
		// look up function
		this.checkType(this.typePool.baseFuncType, recvNode);
		return ((FunctionType) recvNode.getType()).getHandle();
	}

	/**
	 * treat invoke node as method call.
	 * @param node
	 * @return
	 */
	protected Node checkTypeAsMethodCall(InvokeNode node) {
		FieldGetterNode getterNode = (FieldGetterNode) node.getRecvNode();
		String methodName = getterNode.getFieldName();
		ClassType recvType = this.checkAndGetClassType(getterNode.getRecvNode());
		MethodHandle handle = recvType.lookupMethodHandle(methodName);
		if(handle == null) {
			this.throwAndReportTypeError(node, "undefined method: " + methodName);
			return null;
		}
		int paramSize = handle.getParamTypeList().size();
		if(handle.getParamTypeList().size() != node.getArgList().size()) {
			this.throwAndReportTypeError(node, "not match parameter size: method is " + paramSize + ", but params size is " + node.getArgList().size());
		}
		for(int i = 0; i < paramSize; i++) {
			this.checkParamTypeAt(handle.getParamTypeList(), node.getArgList(), i);
		}
		node.setHandle(handle);
		node.setType(handle.getReturnType());
		return node;
	}

	@Override
	public Node visit(InvokeNode node) {
		if(node.getRecvNode() instanceof FieldGetterNode) {
			return this.checkTypeAsMethodCall(node);
		}
		return this.checkTypeAsFuncCall(node);
	}

	@Override
	public Node visit(ConstructorCallNode node) {
		Type recvType = node.getTypeSymbol().toType(this.typePool);
		List<Type> paramTypeList = new ArrayList<>();
		for(ExprNode paramNode : node.getNodeList()) {
			this.checkType(paramNode);
			paramTypeList.add(paramNode.getType());
		}
		ConstructorHandle handle = recvType.lookupConstructorHandle(paramTypeList);
		if(handle == null) {
			StringBuilder sBuilder = new StringBuilder();
			for(Type paramType : paramTypeList) {
				sBuilder.append(" ");
				sBuilder.append(paramType.toString());
			}
			this.throwAndReportTypeError(node, "undefined constructor:" + sBuilder.toString());
			return null;
		}
		int size = handle.getParamTypeList().size();
		for(int i = 0; i < size; i++) {
			this.checkParamTypeAt(handle.getParamTypeList(), node.getNodeList(), i);
		}
		node.setHandle(handle);
		node.setType(handle.getOwnerType());
		return node;
	}

	@Override
	public Node visit(CondOpNode node) {
		String condOp = node.getConditionalOp();
		if(!condOp.equals("&&") && !condOp.equals("||")) {
			this.throwAndReportTypeError(node, "undefined conditional operator: " + condOp);
		}
		this.checkType(this.typePool.booleanType, node.getLeftNode());
		this.checkType(this.typePool.booleanType, node.getRightNode());
		node.setType(this.typePool.booleanType);
		return node;
	}

	@Override
	public Node visit(AssertNode node) {
		this.checkType(this.typePool.booleanType, node.getExprNode());
		OperatorHandle handle = this.opTable.getOperatorHandle(AssertNode.opName, this.typePool.booleanType);
		if(handle == null) {
			this.throwAndReportTypeError(node, "undefined operator: " + AssertNode.opName + " " + this.typePool.booleanType);
		}
		node.setHandle(handle);
		return node;
	}

	@Override
	public Node visit(BlockNode node) {
		int size = node.getNodeList().size();
		for(int i = 0; i < size; i++) {
			Node targetNode = node.getNodeList().get(i);
			this.checkType(targetNode);
			if((targetNode instanceof BlockEndNode) && (i != size - 1)) {
				this.throwAndReportTypeError(node.getNodeList().get(i + 1), "found unreachable code");
			}
		}
		return node;
	}

	@Override
	public Node visit(BreakNode node) {
		this.checkAndThrowIfOutOfLoop(node);
		return node;
	}

	@Override
	public Node visit(ContinueNode node) {
		this.checkAndThrowIfOutOfLoop(node);
		return node;
	}

	@Override
	public Node visit(ExportEnvNode node) {
		this.addEntryAndThrowIfDefined(node, node.getEnvName(), this.typePool.stringType, true);
		this.checkType(this.typePool.stringType, node.getExprNode());
		OperatorHandle handle = this.opTable.getOperatorHandle("setEnv", this.typePool.stringType, this.typePool.stringType);
		if(handle == null) {
			this.throwAndReportTypeError(node, "undefined operator: setEnv");
		}
		node.setHandle(handle);
		return node;
	}

	@Override
	public Node visit(ImportEnvNode node) {
		this.addEntryAndThrowIfDefined(node, node.getEnvName(), this.typePool.stringType, true);
		OperatorHandle handle = this.opTable.getOperatorHandle("getEnv", this.typePool.stringType);
		if(handle == null) {
			this.throwAndReportTypeError(node, "undefined operator: getEnv");
		}
		node.setHandle(handle);
		return node;
	}

	@Override
	public Node visit(ForNode node) {
		this.symbolTable.createAndPushNewTable();
		this.checkType(node.getInitNode());
		this.checkType(this.typePool.booleanType, node.getCondNode());
		this.checkType(node.getIterNode());
		this.checkTypeWithCurrentBlockScope(node.getBlockNode());
		this.symbolTable.popCurrentTable();
		return node;
	}

	@Override
	public Node visit(ForInNode node) {	//TODO: check iterator support, add entry to symbolTable
		this.throwAndReportTypeError(node, "unimplemtned type checker: " + node.getClass().getSimpleName());
		return node;
	}

	@Override
	public Node visit(WhileNode node) {
		this.checkType(this.typePool.booleanType, node.getCondNode());
		this.checkTypeWithNewBlockScope(node.getBlockNode());
		return node;
	}

	@Override
	public Node visit(IfNode node) {
		this.checkType(this.typePool.booleanType, node.getCondNode());
		this.checkTypeWithNewBlockScope(node.getThenBlockNode());
		this.checkTypeWithNewBlockScope(node.getElseBlockNode());
		return node;
	}

	@Override
	public Node visit(ReturnNode node) {
		Type returnType = this.symbolTable.getCurrentReturnType();
		if(returnType instanceof TypePool.UnresolvedType) {
			this.throwAndReportTypeError(node, "only available inside function");
		}
		this.checkType(returnType, node.getExprNode());
		if(node.getExprNode().getType() instanceof TypePool.VoidType) {
			if(!(node.getExprNode() instanceof EmptyNode)) {
				this.throwAndReportTypeError(node, "do not need expression");
			}
		}
		return node;
	}

	@Override
	public Node visit(ThrowNode node) {
		this.checkType(this.typePool.exceptionType, node.getExprNode());
		return node;
	}

	@Override
	public Node visit(TryNode node) {
		this.checkTypeWithNewBlockScope(node.getTryBlockNode());
		for(CatchNode catchNode : node.getCatchNodeList()) {
			this.checkType(catchNode);
		}
		this.checkTypeWithNewBlockScope(node.getFinallyBlockNode());
		return node;
	}

	@Override
	public Node visit(CatchNode node) {
		/**
		 * resolve exception type.
		 */
		TypeSymbol typeSymbol = node.getTypeSymbol();
		Type exceptionType = this.typePool.exceptionType;
		if(typeSymbol != null) {
			exceptionType = typeSymbol.toType(this.typePool);
		}
		if(!this.typePool.exceptionType.isAssignableFrom(exceptionType)) {
			this.throwAndReportTypeError(node, "require exception type");
		}
		node.setExceptionType((ClassType) exceptionType);
		/**
		 * check type catch block.
		 */
		this.symbolTable.createAndPushNewTable();
		this.addEntryAndThrowIfDefined(node, node.getExceptionVarName(), exceptionType, true);
		this.checkTypeWithCurrentBlockScope(node.getCatchBlockNode());
		this.symbolTable.popCurrentTable();
		return node;
	}

	@Override
	public Node visit(VarDeclNode node) {
		this.checkType(node.getInitValueNode());
		this.addEntryAndThrowIfDefined(node, node.getVarName(), node.getInitValueNode().getType(), node.isReadOnly());
		node.setGlobal(node.getParentNode() instanceof RootNode);
		return node;
	}

	@Override
	public Node visit(AssignNode node) {	//TODO: int to float assign
		Type leftType = ((ExprNode) this.checkType(node.getLeftNode())).getType();
		Type rightType = ((ExprNode) this.checkType(node.getRightNode())).getType();
		if(node.getLeftNode().isReadOnly()) {
			this.throwAndReportTypeError(node, "read only variable");
		}
		String op = node.getAssignOp();
		if(op.equals("=")) {
			if(!leftType.isAssignableFrom(rightType)) {
				this.throwAndReportTypeError(node, "illegal assginment: " + rightType + " -> " + leftType);
			}
		} else {
			String opPrefix = op.substring(0, op.length() - 1);
			OperatorHandle handle = this.opTable.getOperatorHandle(opPrefix, leftType, rightType);
			if(handle == null) {
				this.throwAndReportTypeError(node, "undefined self assignment: " + op);
			}
			node.setHandle(handle);
		}
		return node;
	}

	@Override
	public Node visit(FunctionNode node) {
		// create function type and holder type
		int size = node.getArgDeclNodeList().size();
		Type[] paramTypes = new Type[size];
		for(int i = 0; i < size; i++) {
			paramTypes[i] = node.getParamTypeSymbolList().get(i).toType(this.typePool);
		}
		String funcName = node.getFuncName();
		Type returnType = node.getRetunrTypeSymbol().toType(this.typePool);
		FunctionType funcType = this.typePool.createAndGetFuncTypeIfUndefined(returnType, paramTypes);
		FuncHolderType holderType = this.typePool.createFuncHolderType(funcType, funcName);
		this.addEntryAndThrowIfDefined(node, funcName, holderType, true);
		
		// check type func body
		this.symbolTable.pushReturnType(returnType);
		this.symbolTable.createAndPushNewTable();
		for(int i = 0; i < size; i++) {
			this.visitParamDecl(node.getArgDeclNodeList().get(i), paramTypes[i]);
		}
		this.checkTypeWithCurrentBlockScope(node.getBlockNode());
		this.symbolTable.popCurrentTable();
		this.symbolTable.popReturnType();
		node.setHolderType(holderType);
		// check control structure
		this.checkBlockEndExistence(node.getBlockNode(), returnType);
		return null;
	}

	private void visitParamDecl(SymbolNode paramDeclNode, Type paramType) {
		this.addEntryAndThrowIfDefined(paramDeclNode, paramDeclNode.getSymbolName(), paramType, true);
	}

	@Override
	public Node visit(ClassNode node) {
		//TODO
		this.throwAndReportTypeError(node, "unimplemtned type checker: " + node.getClass().getSimpleName());
		return null;
	}

	@Override
	public Node visit(ConstructorNode node) {
		// TODO 
		this.throwAndReportTypeError(node, "unimplemtned type checker: " + node.getClass().getSimpleName());
		return null;
	}

	@Override
	public Node visit(EmptyNode node) {
		node.setType(this.typePool.voidType);
		return node;
	}

	@Override
	public Node visit(EmptyBlockNode node) {
		return node;	// do nothing
	}

	public RootNode checkTypeRootNode(RootNode node) {
		try {
			for(Node targetNode : node.getNodeList()) {
				this.checkType(targetNode);
			}
			OperatorHandle handle = this.opTable.getOperatorHandle(RootNode.opName, this.typePool.objectType, this.typePool.stringType);
			if(handle == null) {
				this.throwAndReportTypeError(node, "undefine operator: " + RootNode.opName + "(" + this.typePool.objectType + ", " + this.typePool.stringType + ")");
			}
			node.setHandle(handle);
			return node;
		} catch(TypeError e) {
			this.symbolTable.popAllLocal();
			System.err.println(e.getMessage());
		}
		return null;
	}

	private void throwAndReportTypeError(Node node, String message) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("[TypeError] ");
		sBuilder.append(message);
		Token token = node.getToken();
		if(token != null) {
			sBuilder.append("\n\t");
			StringBuilder subBuilder = new StringBuilder();
			subBuilder.append(token.getTokenSource().getSourceName());
			subBuilder.append(':');
			subBuilder.append(token.getLine());
			subBuilder.append(',');
			subBuilder.append(token.getCharPositionInLine());
			subBuilder.append("   ");
			String errorLocation = subBuilder.toString();
			int size = errorLocation.length();

			String tokenText = token.getText();
			sBuilder.append(errorLocation);
			sBuilder.append(tokenText);
			sBuilder.append("\n\t");
			for(int i = 0; i < size; i++) {
				sBuilder.append(' ');
			}
			int tokenSize = tokenText.length();
			for(int i = 0; i < tokenSize; i++) {
				sBuilder.append('^');
			}
		}
		throw new TypeError(sBuilder.toString());
	}

	public static class TypeError extends RuntimeException {
		private static final long serialVersionUID = -6490540925854900348L;
		private TypeError(String message) {
			super(message);
		}
	}
}
