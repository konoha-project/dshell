package dshell.internal.parser;

import java.util.ArrayList;

import org.antlr.v4.runtime.Token;

import dshell.internal.parser.NodeUtils.ArgsDecl;
import dshell.internal.parser.NodeUtils.Arguments;
import dshell.internal.parser.NodeUtils.Block;
import dshell.internal.parser.NodeUtils.CatchedException;
import dshell.internal.parser.NodeUtils.ClassBody;
import dshell.internal.parser.NodeUtils.IfElseBlock;
import dshell.internal.parser.NodeUtils.MapEntry;
import dshell.internal.parser.NodeUtils.ReturnExpr;
import dshell.internal.parser.TypePool.Type;

/**
 * Represents dshell grammar element.
 * @author skgchxngsxyz-osx
 *
 */
public class Node {
	/**
	 * For line number generation.
	 */
	protected Token token;

	public void setToken(Token token) {
		this.token = token;
	}

	public Token getToken() {
		return this.token;
	}
	
	
	// ##################
	// #   expression   #
	// ##################
	/**
	 * This node represents expression.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static abstract class ExprNode extends Node {
		/**
		 * If true, this node is considered as statement.
		 * Evaluating after statement, jvm operand stack is empty.
		 */
		protected boolean asStatement = false;

		/**
		 * It represents expression return type.
		 */
		protected Type type = TypePool.getInstance().unresolvedType;

		/**
		 * Consider this node as statement.
		 */
		public final void requireStatement() {
			this.asStatement = true;
		}

		/**
		 * If true, this node is considered as statement.
		 * @return
		 */
		public final boolean isStatement() {
			return this.asStatement;
		}

		/**
		 * Set evaluated value type of this node.
		 * @param type
		 * - this node's evaluated value type
		 */
		public void setType(Type type) {
			this.type = type;
		}

		/**
		 * Get this node's evaluated value type
		 * @return
		 */
		public Type getType() {
			return this.type;
		}
	}

	/**
	 * Represent constant int value.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class IntValueNode extends ExprNode {
		private final long value;

		public IntValueNode(Token token) {
			this.setToken(token);
			this.type = TypePool.getInstance().intType;
			this.value = Long.parseLong(token.getText());
		}

		public long getValue() {
			return this.value;
		}
	}

	/**
	 * Represents constant float value.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FloatValueNode extends ExprNode {
		private final double value;

		public FloatValueNode(Token token) {
			this.setToken(token);
			this.type = TypePool.getInstance().floatType;
			this.value = Double.parseDouble(token.getText());
		}

		public double getValue() {
			return this.value;
		}
	}

	/**
	 * Represents constant boolean value.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class BooleanValueNode extends ExprNode {
		private boolean value;

		public BooleanValueNode(Token token) {
			this.setToken(token);
			this.type = TypePool.getInstance().booleanType;
			this.value = Boolean.parseBoolean(token.getText());
		}

		public boolean getValue() {
			return this.value;
		}
	}

	/**
	 * Represent constant string value.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class StringValueNode extends ExprNode {
		private final String value;

		public StringValueNode(Token token) {
			this.setToken(token);
			this.type = TypePool.getInstance().stringType;
			this.value = parseTokenText(this.token);
		}

		//TODO: escape sequence
		public static String parseTokenText(Token token) {
			return token.getText();
		}

		public String getValue() {
			return this.value;
		}
	}

	/**
	 * Represents null value.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class NullNode extends ExprNode {
		public NullNode(Token token) {
			this.setToken(token);
		}
	}

	/**
	 * This node represents array literal.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ArrayNode extends ExprNode {
		private final ArrayList<Node> nodeList;

		public ArrayNode() {
			this.nodeList = new ArrayList<>();
		}

		public void addNode(Node node) {
			this.nodeList.add(node);
		}

		public ArrayList<Node> getNodeList() {
			return this.nodeList;
		}
	}

	/**
	 * This node represents map literal.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class MapNode extends ExprNode {
		private final ArrayList<Node> keyList;
		private final ArrayList<Node> valueList;

		public MapNode() {
			this.keyList = new ArrayList<>();
			this.valueList = new ArrayList<>();
		}

		public void addEntry(MapEntry entry) {
			this.keyList.add(entry.keyNode);
			this.valueList.add(entry.valueNode);
		}

		public ArrayList<Node> getKeyList() {
			return this.keyList;
		}

		public ArrayList<Node> getValueList() {
			return this.valueList;
		}
	}

	/**
	 * This node represents local variable.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class SymbolNode extends ExprNode {
		private final String symbolName;

		public SymbolNode(Token token) {
			this.setToken(token);
			this.symbolName = this.token.getText();
		}

		public String getSymbolName() {
			return this.symbolName;
		}
	}

	/**
	 * This node represents getting array element.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ElementGetterNode extends ExprNode {
		private final Node recvNode;
		private final Node indexNode;

		public ElementGetterNode(Node recvNode, Node indexNode) {
			this.recvNode = recvNode;
			this.indexNode = indexNode;
		}

		public Node getRecvNode() {
			return this.recvNode;
		}

		public Node getIndexNode() {
			return this.indexNode;
		}
	}

	/**
	 * This node represents getting class field.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FieldGetterNode extends ExprNode {
		private final Node recvNode;
		private final String fieldName;

		public FieldGetterNode(Node recvNode, Token token) {
			this.setToken(token);
			this.recvNode = recvNode;
			this.fieldName = this.token.getText();
		}

		public Node getRecvNode() {
			return this.recvNode;
		}

		public String getFieldName() {
			return this.fieldName;
		}
	}

	/**
	 * This node represents type cast.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class CastNode extends ExprNode {
		private final Type targetType;
		private final Node exprNode;

		public CastNode(Type targetType, Node exprNode) {
			this.targetType = targetType;
			this.exprNode = exprNode;
		}

		public Type getTargetType() {
			return this.targetType;
		}

		public Node getExprNode() {
			return this.exprNode;
		}
	}

	/**
	 * Represents instanceof operator.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class InstanceofNode extends ExprNode {
		private final Node exprNode;
		private final Type targetType;

		public InstanceofNode(Token token, Node exprNode, Type targetType) {
			this.setToken(token);
			this.type = TypePool.getInstance().booleanType;
			this.exprNode = exprNode;
			this.targetType = targetType;
		}

		public Node getExprNode() {
			return this.exprNode;
		}

		public Type getTargetType() {
			return this.targetType;
		}
	}

	/**
	 * This node represents suffix increment ('++', '--').
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class SuffixIncrementNode extends ExprNode {
		private final Node exprNode;
		private final String op;

		public SuffixIncrementNode(Node exprNode, Token token) {
			this.setToken(token);
			this.exprNode = exprNode;
			this.op = this.token.getText();
		}

		public Node getExprNode() {
			return this.exprNode;
		}

		public String getOperator() {
			return this.op;
		}
	}

	/**
	 * This node represents function call.
	 * Function includes binary op, prefix op, or user defined function.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FuncCallNode extends ExprNode {
		private final String funcName;
		private final ArrayList<Node> argNodeList;

		public FuncCallNode(Token token, Arguments args) {
			this.setToken(token);
			this.funcName = this.token.getText();
			this.argNodeList = args.nodeList;
		}

		/**
		 * For prefix op
		 * @param token
		 * - operator token
		 * @param node
		 * - operand
		 */
		public FuncCallNode(Token token, Node node) {
			this.setToken(token);
			this.funcName = this.token.getText();
			this.argNodeList = new ArrayList<>();
			this.argNodeList.add(node);
		}

		/**
		 * For binary op
		 * @param token
		 * - operator token
		 * @param leftNode
		 * - binary left
		 * @param rightNode
		 * - binary right
		 */
		public FuncCallNode(Token token, Node leftNode, Node rightNode) {
			this.setToken(token);
			this.funcName = this.token.getText();
			this.argNodeList = new ArrayList<>();
			this.argNodeList.add(leftNode);
			this.argNodeList.add(rightNode);
		}

		public String getFuncName() {
			return this.funcName;
		}

		public ArrayList<Node> getNodeList() {
			return this.argNodeList;
		}
	}

	/**
	 * This node represents instance method call.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class MethodCallNode extends ExprNode {
		private final Node recvNode;
		private final String methodName;
		private final ArrayList<Node> argNodeList;

		public MethodCallNode(Node recvNode, Token token, Arguments args) {
			this.setToken(token);
			this.recvNode = recvNode;
			this.methodName = this.token.getText();
			this.argNodeList = args.nodeList;
		}

		public Node getRecvNode() {
			return this.recvNode;
		}

		public String getMethodName() {
			return this.methodName;
		}

		public ArrayList<Node> getNodeList() {
			return this.argNodeList;
		}
	}

	/**
	 * This node represents class constructor call.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ConstructorCallNode extends ExprNode {
		private final String className;
		private final ArrayList<Node> argNodeList;

		public ConstructorCallNode(Token token, String className, Arguments args) {
			this.setToken(token);
			this.className = className;
			this.argNodeList = args.nodeList;
		}

		public String getClassName() {
			return this.className;
		}

		public ArrayList<Node> getNodeList() {
			return this.argNodeList;
		}
	}

	/**
	 * This node represents conditional 'and' or 'or' operator.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class CondOpNode extends ExprNode {
		private final String condOp;
		private final Node leftNode;
		private final Node rightNode;

		public CondOpNode(Token token, Node leftNode, Node rightNode) {
			this.type = TypePool.getInstance().booleanType;
			this.setToken(token);
			this.condOp = this.token.getText();
			this.leftNode = leftNode;
			this.rightNode = rightNode;
		}

		public String getConditionalOp() {
			return this.condOp;
		}

		public Node getLeftNode() {
			return this.leftNode;
		}

		public Node getRightNode() {
			return this.rightNode;
		}
	}

	//TODO: adding shell command represent nodes

	// #################
	// #   statement   #
	// #################
	/**
	 * This node represents assertion.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class AssertNode extends Node {
		private final Node exprNode;

		public AssertNode(Token token, Node exprNode) {
			this.setToken(token);
			this.exprNode = exprNode;
		}

		public Node getExprNode() {
			return this.exprNode;
		}
	}

	/**
	 * This node represents block.
	 * It contains several statements.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class BlockNode extends Node {
		private final ArrayList<Node> nodeList;

		public BlockNode(Block block) {
			this.nodeList = block.getNodeList();
		}

		public ArrayList<Node> getNodeList() {
			return this.nodeList;
		}
	}

	/**
	 * This node represents break statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class BreakNode extends Node {
		public BreakNode(Token token) {
			this.setToken(token);
		}
	}

	/**
	 * This node represents continue statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ContinueNode extends Node {
		public ContinueNode(Token token) {
			this.setToken(token);
		}
	}

	/**
	 * This node represents exporting variable as environmental variable.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ExportEnvNode extends Node {
		private final String envName;
		private final Node exprNode;

		public ExportEnvNode(Token token, Node exprNode) {
			this.setToken(token);
			this.envName = this.token.getText();
			this.exprNode = exprNode;
		}

		public String getEnvName() {
			return this.envName;
		}

		public Node getExprNode() {
			return this.exprNode;
		}
	}

	/**
	 * This node represents importing environmental variable.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ImportEnvNode extends Node {
		private final String envName;

		public ImportEnvNode(Token token) {
			this.setToken(token);
			this.envName = this.token.getText();
		}

		public String getEnvName() {
			return this.envName;
		}
	}

	/**
	 * This node represents for statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ForNode extends Node {
		/**
		 * May be EmptyNode
		 */
		private final Node initNode;

		/**
		 * May be EmptyNode
		 */
		private final Node condNode;

		/**
		 * May be EmptyNode
		 */
		private final Node iterNode;
		private final BlockNode blockNode;

		public ForNode(Token token, Node initNode, Node condNode, Node iterNode, Node blockNode) {
			this.setToken(token);
			this.initNode = initNode;
			this.condNode = condNode;
			this.iterNode = iterNode;
			this.blockNode = (BlockNode) blockNode;
		}

		public Node getInitNode() {
			return this.initNode;
		}

		public Node getCondNode() {
			return this.condNode;
		}

		public Node getIterNode() {
			return this.iterNode;
		}

		public BlockNode getBlockNode() {
			return this.blockNode;
		}
	}

	/**
	 * This node represents for in statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ForInNode extends Node {
		private final String initName;
		private final Node exprNode;
		private final BlockNode blockNode;

		public ForInNode(Token token, Token nameToken, Node exprNode, Node blockNode) {
			this.setToken(token);
			this.initName = nameToken.getText();
			this.exprNode = exprNode;
			this.blockNode = (BlockNode) blockNode;
		}

		public String getInitName() {
			return this.initName;
		}

		public Node getExprNode() {
			return this.exprNode;
		}

		public BlockNode getBlockNode() {
			return this.blockNode;
		}
	}

	/**
	 * This node represents while statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class WhileNode extends Node {
		private final Node condNode;
		private final BlockNode blockNode;

		public WhileNode(Token token, Node condNode, Node blockNode) {
			this.setToken(token);
			this.condNode = condNode;
			this.blockNode = (BlockNode) blockNode;
		}

		public Node getCondNode() {
			return this.condNode;
		}

		public BlockNode getBlockNode() {
			return this.blockNode;
		}
	}

	/**
	 * This node represents if statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class IfNode extends Node {
		private final Node condNode;
		private final BlockNode thenBlockNode;
		/**
		 * May be EmptyblockNode
		 */
		private final BlockNode elseBlockNode;

		public IfNode(Token token, Node condNode, IfElseBlock block) {
			this.setToken(token);
			this.condNode = condNode;
			this.thenBlockNode = block.getThenBlockNode();
			this.elseBlockNode = block.getElseBlockNode();
		}

		public Node getCondNode() {
			return this.condNode;
		}

		public BlockNode getThenBlockNode() {
			return this.thenBlockNode;
		}

		public BlockNode getElseBlockNode() {
			return this.elseBlockNode;
		}
	}

	/**
	 * This node represents return statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ReturnNode extends Node {
		/**
		 * May be EmptyNode.
		 */
		private final Node exprNode;

		public ReturnNode(Token token, ReturnExpr returnExpr) {
			this.setToken(token);
			this.exprNode = returnExpr.getExprNode();
		}

		public Node getExprNode() {
			return this.exprNode;
		}
	}

	/**
	 * This node represents throw statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ThrowNode extends Node {
		private final Node exprNode;

		public ThrowNode(Token token, Node exprNode) {
			this.setToken(token);
			this.exprNode = exprNode;
		}

		public Node getExprNode() {
			return this.exprNode;
		}
	}

	/**
	 * This node represents try-catch statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class TryNode extends Node {
		private final BlockNode tryBlockNode;
		private final ArrayList<CatchNode> catchNodeList;
		/**
		 * May be EmptyBlockNode.
		 */
		private final BlockNode finallyBlockNode;

		public TryNode(Token token, Node tryBlockNode, Node finallyBlockNode) {
			this.setToken(token);
			this.tryBlockNode = (BlockNode) tryBlockNode;
			this.catchNodeList = new ArrayList<>();
			this.finallyBlockNode = (BlockNode) finallyBlockNode;
		}

		public BlockNode getTryBlockNode() {
			return this.tryBlockNode;
		}

		/**
		 * Only called from dshellParser.
		 * Do not call it.
		 * @param node
		 */
		public void setCatchNode(CatchNode node) {
			this.catchNodeList.add(node);
		}

		public ArrayList<CatchNode> getCatchNodeList() {
			return this.catchNodeList;
		}

		public BlockNode getFinallyBlockNode() {
			return this.finallyBlockNode;
		}
	}

	/**
	 * This node represents catch statement.
	 * It always exists in TryNode.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class CatchNode extends Node {
		private final Type exceptionType;
		private final String exceptionName;
		private final BlockNode catchBlockNode;

		public CatchNode(Token token, CatchedException catchedExcept, Node catchBlockNode) {
			this.setToken(token);
			this.exceptionType = catchedExcept.getType();
			this.exceptionName = catchedExcept.getName();
			this.catchBlockNode = (BlockNode) catchBlockNode;
		}

		public Type getExceptionType() {
			return this.exceptionType;
		}

		public String getExceptionVarName() {
			return this.exceptionName;
		}

		public BlockNode getCatchBlockNode() {
			return this.catchBlockNode;
		}
	}

	/**
	 * This node represents variable declaration.
	 * It requires initial value.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class VarDeclNode extends Node {
		public final boolean isReadOnly;
		private final String varName;
		private final Node initValueNode;
		private Type valueType;

		public VarDeclNode(Token token, Token nameToken, Node initValueNode) {
			this.setToken(token);
			this.varName = nameToken.getText();
			this.initValueNode = initValueNode;
			this.valueType = TypePool.getInstance().unresolvedType;
			this.isReadOnly = !token.getText().equals("var");
		}

		public String getVarName() {
			return this.varName;
		}

		public Node getInitValueNode() {
			return this.initValueNode;
		}

		public void setValueType(Type type) {
			this.valueType = type;
		}

		public Type getValueType() {
			return this.valueType;
		}
	}

	/**
	 * This node represents assign operation such as '=', '+=', '-=', '*=', '/=', '%='.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class AssignNode extends Node {
		private final String assignOp;
		/**
		 * requires SymbolNode, ElementGetterNode or FieldGetterNode.
		 */
		private final Node leftNode;
		private final Node rightNode;

		public AssignNode(Token token, Node leftNode, Node rightNode) {
			this.setToken(token);
			this.assignOp = this.token.getText();
			this.leftNode = leftNode;
			this.rightNode = rightNode;
		}

		public String getAssignOp() {
			return this.assignOp;
		}

		public Node getLeftNode() {
			return this.leftNode;
		}

		public Node getRightNode() {
			return this.rightNode;
		}
	}

	/**
	 * This node represents function.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FunctionNode extends Node {
		private final String funcName;
		private final ArrayList<SymbolNode> nodeList;
		private final BlockNode blockNode;

		public FunctionNode(Token token, Token nameToken, ArgsDecl decl, Node blockNode) {
			this.setToken(token);
			this.funcName = nameToken.getText();
			this.nodeList = decl.getNodeList();
			this.blockNode = (BlockNode) blockNode;
		}

		public String getFuncName() {
			return this.funcName;
		}

		public ArrayList<SymbolNode> getArgDeclNodeList() {
			return this.nodeList;
		}

		public BlockNode getBlockNode() {
			return this.blockNode;
		}
	}

	/**
	 * This node represents class.
	 * It contains class field, instance method, constructor.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ClassNode extends Node {
		private final String className;
		private final ArrayList<Node> classElementList;

		public ClassNode(Token token, String className, Type superType, ClassBody body) {
			this.setToken(token);
			this.classElementList = body.getNodeList();
			/**
			 * in ClassNode initialization, set ClassType to TypePool.
			 */
			this.className = className;
			TypePool.getInstance().createClassType(this.className, superType);
		}

		public String getClassName() {
			return this.className;
		}

		public ArrayList<Node> getElementList() {
			return this.classElementList;
		}
	}

	/**
	 * Represents class constructor.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ConstructorNode extends Node {
		private Type recvType;
		private final ArrayList<SymbolNode> nodeList;
		private final BlockNode blockNode;

		public ConstructorNode(Token token, ArgsDecl decl, Node blockNode) {
			this.setToken(token);
			this.recvType = TypePool.getInstance().unresolvedType;
			this.nodeList = decl.getNodeList();
			this.blockNode = (BlockNode) blockNode;
		}

		public void setRecvType(Type type) {
			this.recvType = type;
		}

		public Type getRecvType() {
			return this.recvType;
		}

		public ArrayList<SymbolNode> getArgDeclNodeList() {
			return this.nodeList;
		}

		public BlockNode getBlockNode() {
			return this.blockNode;
		}
	}

	/**
	 * Represents empty statement.
	 * It is ignored in type checking or code generation.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class EmptyNode extends Node {
	}

	/**
	 * Represent empty block.
	 * It id ignored in type checking or code generation.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class EmptyBlockNode extends BlockNode {
		public EmptyBlockNode() {
			super(new Block());
		}
	}

	/**
	 * Represent root node.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class RootNode extends Node {
		private final ArrayList<Node> nodeList;

		public RootNode() {
			this.nodeList = new ArrayList<>();
		}

		public void addNode(Node node) {
			this.nodeList.add(node);
		}

		public ArrayList<Node> getNodeList() {
			return this.nodeList;
		}
	}
}
