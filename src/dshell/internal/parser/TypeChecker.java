package dshell.internal.parser;

import java.util.List;

import org.omg.CORBA.PUBLIC_MEMBER;

import dshell.internal.parser.CalleeHandle.ConstructorHandle;
import dshell.internal.parser.CalleeHandle.FieldHandle;
import dshell.internal.parser.CalleeHandle.FunctionHandle;
import dshell.internal.parser.CalleeHandle.MethodHandle;
import dshell.internal.parser.CalleeHandle.OperatorHandle;
import dshell.internal.parser.Node.ArrayNode;
import dshell.internal.parser.Node.AssertNode;
import dshell.internal.parser.Node.AssignNode;
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
import dshell.internal.parser.Node.FuncCallNode;
import dshell.internal.parser.Node.FunctionNode;
import dshell.internal.parser.Node.IfNode;
import dshell.internal.parser.Node.ImportEnvNode;
import dshell.internal.parser.Node.InstanceofNode;
import dshell.internal.parser.Node.IntValueNode;
import dshell.internal.parser.Node.MapNode;
import dshell.internal.parser.Node.MethodCallNode;
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
import dshell.internal.parser.TypePool.FunctionType;
import dshell.internal.parser.TypePool.Type;
import dshell.internal.parser.TypePool.UnresolvedType;

public class TypeChecker implements NodeVisitor<Node>{
	private final TypePool typePool;
	private final SymbolTable symbolTable;
	private final AbstractOperatorTable opTable;
	private Type requiredType;

	public TypeChecker(TypePool typePool) {
		this.typePool = typePool;
		this.symbolTable = new SymbolTable();
		this.opTable = new OperatorTable(this.typePool);
	}

	private boolean hasUnresolvedType(ExprNode targetNode) {
		return targetNode.getType() instanceof UnresolvedType;
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
		if(!(targetNode instanceof ExprNode)) {
			return targetNode.accept(this);
		}
		if(!this.hasUnresolvedType((ExprNode) targetNode)) {
			return targetNode;
		}
		if(requiredType == null) {
			return targetNode.accept(this);
		}
		ExprNode checkedNode = (ExprNode) targetNode.accept(this);
		if(requiredType.isAssignableFrom(checkedNode.getType())) {
			return checkedNode;
		}
		this.throwAndReportTypeError(checkedNode, "require " + requiredType + ", but is " + checkedNode.getType());
		return null;
	}

	/**
	 * checl node type
	 * @param targetNode
	 * @return
	 * - if targteNode is ExprNode, treat it as statement.
	 */
	private Node checkTypeAsStatement(Node targetNode) {
		Node checkedNode = this.checkType(targetNode);
		if(checkedNode instanceof ExprNode) {
			((ExprNode)checkedNode).requireStatement();
		}
		return checkedNode;
	}

	private ClassType checkAndGetClassType(ExprNode recvNode) {
		this.checkType(this.typePool.objectType, recvNode);
		Type resolvedType = recvNode.getType();
		if(!(resolvedType instanceof ClassType)) {
			this.throwAndReportTypeError(recvNode, "require class tyep, but is " + resolvedType);
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
		paramNodeList.add(checkedNode);
	}

	/**
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

	private void setPropertyToSymbolNode(SymbolNode node) {
		SymbolEntry entry = this.symbolTable.getEntry(node.getSymbolName());
		if(entry == null) {
			this.throwAndReportTypeError(node, "undefined symbol: " + node.getSymbolName());
		}
		node.setGlobal(entry.isGlobal());
		node.setReadOnly(entry.isReadOnly());
		node.setType(entry.getType());
	}

	private void addEntryAndThrowIfDefined(Node node, String symbolName, Type type, boolean isReadOnly) {
		if(!this.symbolTable.addEntry(symbolName, type, isReadOnly)) {
			this.throwAndReportTypeError(node, symbolName + " is already defined");
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
	public Node visit(NullNode node) {	//TODO: context type
		return null;
	}

	@Override
	public Node visit(ArrayNode node) {	//TODO: empty array handling
		int elementSize = node.getNodeList().size();
		assert elementSize != 0;
		ExprNode firstElementNode = node.getNodeList().get(0);
		this.checkType(firstElementNode);
		Type elementType = firstElementNode.getType();
		for(int i = 1; i < elementSize; i++) {
			this.checkType(elementType, node.getNodeList().get(i));
		}
		node.setType(this.typePool.createAndGetArrayTypeIfUndefined(elementType));
		return node;
	}

	@Override
	public Node visit(MapNode node) { // TODO: empty map handling
		int entrySize = node.getKeyList().size();
		assert entrySize != 0;
		ExprNode firstValueNode = node.getValueList().get(0);
		this.checkType(firstValueNode);
		Type valueType = firstValueNode.getType();
		for(int i = 0; i < entrySize; i++) {
			this.checkType(this.typePool.stringType, node.getKeyList().get(i));
			this.checkType(valueType, node.getValueList().get(i));
		}
		node.setType(this.typePool.createAndGetMapTypeIfUndefined(valueType));
		return node;
	}

	@Override
	public Node visit(SymbolNode node) {	//TODO: func object
		this.setPropertyToSymbolNode(node);
		return node;
	}

	@Override
	public Node visit(ElementGetterNode node) {	//TODO: method handle property
		return null;
	}

	@Override
	public Node visit(FieldGetterNode node) {
		ClassType recvType = this.checkAndGetClassType(node.getRecvNode());
		FieldHandle handle = recvType.getFieldHandleMap().get(node.getFieldName());
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
		this.checkType(node.getExprNode());
		Type exprType = node.getExprNode().getType();
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

	@Override
	public Node visit(FuncCallNode node) {	//TODO: static func call
		SymbolEntry entry = this.symbolTable.getEntry(node.getFuncName());
		if(entry == null) {
			this.throwAndReportTypeError(node, "undefined function: " + node.getFuncName());
		}
		Type type = entry.getType();
		if(!(type instanceof FunctionType)) {
			this.throwAndReportTypeError(node, node.getFuncName() + " is not function");
		}
		FunctionHandle handle = ((FunctionType) type).getHandle();
		int paramSize = handle.getParamTypeList().size();
		if(handle.getParamTypeList().size() != node.getNodeList().size()) {
			this.throwAndReportTypeError(node, "not match parameter size: function is " + paramSize + "but params size is " + node.getNodeList().size());
		}
		for(int i = 0; i < paramSize; i++) {
			this.checkParamTypeAt(handle.getParamTypeList(), node.getNodeList(), i);
		}
		node.setHandle(handle);
		node.setType(handle.getReturnType());
		return node;
	}

	@Override
	public Node visit(MethodCallNode node) {
		ClassType recvType = this.checkAndGetClassType(node.getRecvNode());
		MethodHandle handle = recvType.getMethodHandleMap().get(node.getMethodName());
		if(handle == null) {
			this.throwAndReportTypeError(node, "undefined method: " + node.getMethodName());
			return null;
		}
		int paramSize = handle.getParamTypeList().size();
		if(handle.getParamTypeList().size() != node.getNodeList().size()) {
			this.throwAndReportTypeError(node, "not match parameter size: method is " + paramSize + "but params size is " + node.getNodeList().size());
		}
		for(int i = 0; i < paramSize; i++) {
			this.checkParamTypeAt(handle.getParamTypeList(), node.getNodeList(), i);
		}
		node.setHandle(handle);
		node.setType(handle.getReturnType());
		return node;
	}

	@Override
	public Node visit(ConstructorCallNode node) {	//TODO: match parameter
		ClassType classType = this.typePool.getClassType(node.getClassName());
		
		return null;
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
		for(Node statementNode : node.getNodeList()) {
			this.checkTypeAsStatement(statementNode);
		}
		return node;
	}

	@Override
	public Node visit(BreakNode node) {	//TODO: check inside loop
		return node;
	}

	@Override
	public Node visit(ContinueNode node) {	//TODO: check inside loop
		return node;
	}

	@Override
	public Node visit(ExportEnvNode node) {
		this.checkType(this.typePool.stringType, node.getExprNode());
		return node;
	}

	@Override
	public Node visit(ImportEnvNode node) {
		this.symbolTable.addEntry(node.getEnvName(), this.typePool.stringType, true);
		return node;
	}

	@Override
	public Node visit(ForNode node) {	//TODO: add entry to symbolTable
		this.symbolTable.createAndPushNewTable();
		this.checkTypeAsStatement(node.getInitNode());
		this.checkType(this.typePool.booleanType, node.getCondNode());
		this.checkTypeAsStatement(node.getIterNode());
		this.checkTypeWithCurrentBlockScope(node.getBlockNode());
		this.symbolTable.popCurrentTable();
		return node;
	}

	@Override
	public Node visit(ForInNode node) {	//TODO: check iterator support, add entry to symbolTable
		
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
	public Node visit(ReturnNode node) {	//TODO: check inside function
		this.checkType(node.getExprNode());
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
		String op = node.getAssignOp();
		Type leftType = ((ExprNode) this.checkType(node.getLeftNode())).getType();
		Type rightType = ((ExprNode) this.checkType(node.getRightNode())).getType();
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
	public Node visit(FunctionNode node) {	//TODO:
		return null;
	}

	@Override
	public Node visit(ClassNode node) {
		//TODO
		return null;
	}

	@Override
	public Node visit(ConstructorNode node) {
		// TODO 
		return null;
	}

	@Override
	public Node visit(EmptyNode node) {
		return node;	//do nothing
	}

	@Override
	public Node visit(EmptyBlockNode node) {
		return node;	// do nothing
	}

	public RootNode checkTypeRootNode(RootNode node) {
		try {
			for(Node statementNode : node.getNodeList()) {
				this.checkTypeAsStatement(statementNode);
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
		throw new TypeError(message);
	}

	public static class TypeError extends RuntimeException {
		private static final long serialVersionUID = -6490540925854900348L;
		private TypeError(String message) {
			super(message);
		}
	}
}
