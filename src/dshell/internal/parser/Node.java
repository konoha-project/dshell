package dshell.internal.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import dshell.internal.parser.CalleeHandle.FieldHandle;
import dshell.internal.parser.CalleeHandle.FunctionHandle;
import dshell.internal.parser.CalleeHandle.MethodHandle;
import dshell.internal.parser.ParserUtils.ArgsDecl;
import dshell.internal.parser.ParserUtils.Arguments;
import dshell.internal.parser.ParserUtils.Block;
import dshell.internal.parser.ParserUtils.IfElseBlock;
import dshell.internal.parser.ParserUtils.ReturnExpr;
import dshell.internal.parser.TypePool.ClassType;
import dshell.internal.parser.TypePool.Type;

/**
 * Represents dshell grammar element.
 * @author skgchxngsxyz-osx
 *
 */
public abstract class Node {
	/**
	 * For line number generation.
	 */
	protected Token token;

	protected Node parentNode;

	public void setToken(Token token) {
		this.token = token;
	}

	public Token getToken() {
		return this.token;
	}

	/**
	 * set parent node reference
	 * @param parentNode
	 */
	public void setParentNode(Node parentNode) {
		this.parentNode = parentNode;
	}

	/**
	 * set node as this node's child
	 * @param childNode
	 * @return
	 * - childNode
	 */
	public Node setNodeAsChild(Node childNode) {
		/**
		 * set child node parent.
		 */
		childNode.setParentNode(this);
		return childNode;
	}

	/**
	 * get parent node.
	 * @return
	 * - return null, if this node is RootNode.
	 */
	public Node getParentNode() {
		return this.parentNode;
	}

	abstract public <T> T accept(NodeVisitor<T> visitor);
	
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
		protected Type type = TypePool.unresolvedType;

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
			this.value = Long.parseLong(token.getText());
		}

		public long getValue() {
			return this.value;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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
			this.value = Double.parseDouble(token.getText());
		}

		public double getValue() {
			return this.value;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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
			this.value = Boolean.parseBoolean(token.getText());
		}

		public boolean getValue() {
			return this.value;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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
			this.value = parseTokenText(this.token);
		}

		//TODO: escape sequence
		public static String parseTokenText(Token token) {
			return token.getText();
		}

		public String getValue() {
			return this.value;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents array literal.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ArrayNode extends ExprNode {
		private final List<ExprNode> nodeList;

		public ArrayNode() {
			this.nodeList = new ArrayList<>();
		}

		public void addNode(Node node) {
			this.nodeList.add((ExprNode) this.setNodeAsChild(node));
		}

		public List<ExprNode> getNodeList() {
			return this.nodeList;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents map literal.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class MapNode extends ExprNode {
		private final List<ExprNode> keyList;
		private final List<ExprNode> valueList;

		public MapNode() {
			this.keyList = new ArrayList<>();
			this.valueList = new ArrayList<>();
		}

		public void addEntry(Node keyNode, Node valueNode) {
			this.keyList.add((ExprNode) this.setNodeAsChild(keyNode));
			this.valueList.add((ExprNode) this.setNodeAsChild(valueNode));
		}

		public List<ExprNode> getKeyList() {
			return this.keyList;
		}

		public List<ExprNode> getValueList() {
			return this.valueList;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * Represent left node of AssignNode.
	 * @author skgchxngsxyz-opensuse
	 *
	 */
	public static abstract class AssignableNode extends ExprNode {
	}
	/**
	 * This node represents local variable.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class SymbolNode extends AssignableNode {
		private final String symbolName;
		private TypeSymbol typeSymbol;
		private boolean isGlobal;
		private boolean isReadOnly;

		public SymbolNode(Token token) {
			this.setToken(token);
			this.symbolName = this.token.getText();
			this.isGlobal = false;
			this.isReadOnly = false;
		}

		public String getSymbolName() {
			return this.symbolName;
		}

		public void setTypeSymbol(TypeSymbol typeSymbol) {
			this.typeSymbol = typeSymbol;
		}

		/**
		 * get type symbol
		 * @return
		 * - return null, if has no TypeSymbol.
		 */
		public TypeSymbol getTypeSymbol() {
			return this.typeSymbol;
		}

		public void setReadOnly(boolean isReadOnly) {
			this.isReadOnly = isReadOnly;
		}

		public boolean isReadOnly() {
			return this.isReadOnly;
		}

		public void setGlobal(boolean isGlobal) {
			this.isGlobal = isGlobal;
		}

		public boolean isGlobal() {
			return this.isGlobal;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents getting array element.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ElementGetterNode extends AssignableNode {
		private final ExprNode recvNode;
		private final ExprNode indexNode;
		private MethodHandle handle;

		public ElementGetterNode(Node recvNode, Node indexNode) {
			this.recvNode = (ExprNode) this.setNodeAsChild(recvNode);
			this.indexNode = (ExprNode) this.setNodeAsChild(indexNode);
		}

		public ExprNode getRecvNode() {
			return this.recvNode;
		}

		public ExprNode getIndexNode() {
			return this.indexNode;
		}

		public void setHandle(MethodHandle handle) {
			this.handle = handle;
		}

		public MethodHandle getHandle() {
			return this.handle;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents getting class field.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FieldGetterNode extends AssignableNode {
		private final ExprNode recvNode;
		private final String fieldName;
		private FieldHandle handle;

		public FieldGetterNode(Node recvNode, Token token) {
			this.setToken(token);
			this.recvNode = (ExprNode) this.setNodeAsChild(recvNode);
			this.fieldName = this.token.getText();
		}

		public ExprNode getRecvNode() {
			return this.recvNode;
		}

		public String getFieldName() {
			return this.fieldName;
		}

		public void setHandle(FieldHandle handle) {
			this.handle = handle;
		}

		public FieldHandle getHandle() {
			return this.handle;
		}
		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents type cast and primitive type boxing.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class CastNode extends ExprNode {
		private Type targetType;
		private final TypeSymbol targetTypeSymbol;
		private final ExprNode exprNode;

		public CastNode(TypeSymbol targetTypeSymbol, Node exprNode) {
			this.targetTypeSymbol = targetTypeSymbol;
			this.exprNode = (ExprNode) this.setNodeAsChild(exprNode);
		}

		public TypeSymbol getTypeSymbol() {
			return this.targetTypeSymbol;
		}

		public void setTargteType(Type type) {
			this.targetType = type;
		}

		public Type getTargetType() {
			return this.targetType;
		}

		public ExprNode getExprNode() {
			return this.exprNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * Represents instanceof operator.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class InstanceofNode extends ExprNode {
		private final ExprNode exprNode;
		private final TypeSymbol typeSymbol;
		private Type targetType;

		public InstanceofNode(Token token, Node exprNode, TypeSymbol targetTypeSymbol) {
			this.setToken(token);
			this.exprNode = (ExprNode) this.setNodeAsChild(exprNode);
			this.typeSymbol = targetTypeSymbol;
		}

		public ExprNode getExprNode() {
			return this.exprNode;
		}

		public TypeSymbol getTypeSymbol() {
			return this.typeSymbol;
		}

		public void setTargetType(Type type) {
			this.targetType = type;
		}

		public Type getTargetType() {
			return this.targetType;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents suffix increment ('++', '--').
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class SuffixIncrementNode extends ExprNode {
		private final ExprNode exprNode;
		private final String op;

		public SuffixIncrementNode(Node exprNode, Token token) {
			this.setToken(token);
			this.exprNode = (ExprNode) this.setNodeAsChild(exprNode);
			this.op = this.token.getText();
		}

		public ExprNode getExprNode() {
			return this.exprNode;
		}

		public String getOperator() {
			return this.op;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * Represnts operator( binary op, unary op) call.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class OperatorCallNode extends ExprNode {
		private final String funcName;
		private final List<ExprNode> argNodeList;
		private final List<Type> argTyeList;

		/**
		 * For prefix op
		 * @param token
		 * - operator token
		 * @param node
		 * - operand
		 */
		public OperatorCallNode(Token token, Node node) {
			this.setToken(token);
			this.funcName = this.token.getText();
			this.argNodeList = new ArrayList<>();
			this.argNodeList.add((ExprNode) this.setNodeAsChild(node));
			this.argTyeList = new ArrayList<>();
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
		public OperatorCallNode(Token token, Node leftNode, Node rightNode) {
			this.setToken(token);
			this.funcName = this.token.getText();
			this.argTyeList = new ArrayList<>();
			this.argNodeList = new ArrayList<>();
			this.argNodeList.add((ExprNode) this.setNodeAsChild(leftNode));
			this.argNodeList.add((ExprNode) this.setNodeAsChild(rightNode));
		}

		public String getFuncName() {
			return this.funcName;
		}

		public List<ExprNode> getNodeList() {
			return this.argNodeList;
		}

		public List<Type> getArgTypeList() {
			return this.argTyeList;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents function call.
	 * call user defined function.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FuncCallNode extends ExprNode {
		private final String funcName;
		private final List<ExprNode> argNodeList;
		private FunctionHandle handle;

		public FuncCallNode(Token token, Arguments args) {
			this.setToken(token);
			this.funcName = this.token.getText();
			this.argNodeList = new ArrayList<>();
			for(Node node : args.nodeList) {
				argNodeList.add((ExprNode) this.setNodeAsChild(node));
			}
		}

		public String getFuncName() {
			return this.funcName;
		}

		public List<ExprNode> getNodeList() {
			return this.argNodeList;
		}

		public void setHandle(FunctionHandle handle) {
			this.handle = handle;
		}

		public FunctionHandle getHandle() {
			return this.handle;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents instance method call.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class MethodCallNode extends ExprNode {
		private final ExprNode recvNode;
		private final String methodName;
		private final List<ExprNode> argNodeList;
		private MethodHandle handle;

		public MethodCallNode(Node recvNode, Token token, Arguments args) {
			this.setToken(token);
			this.recvNode = (ExprNode) this.setNodeAsChild(recvNode);
			this.methodName = this.token.getText();
			this.argNodeList = new ArrayList<>();
			for(Node node : args.nodeList) {
				argNodeList.add((ExprNode) this.setNodeAsChild(node));
			}
		}

		public ExprNode getRecvNode() {
			return this.recvNode;
		}

		public String getMethodName() {
			return this.methodName;
		}

		public List<ExprNode> getNodeList() {
			return this.argNodeList;
		}

		public void setHandle(MethodHandle handle) {
			this.handle = handle;
		}

		public MethodHandle getHandle() {
			return this.handle;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents class constructor call.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ConstructorCallNode extends ExprNode {
		private final String className;
		private final List<ExprNode> argNodeList;

		public ConstructorCallNode(Token token, String className, Arguments args) {
			this.setToken(token);
			this.className = className;
			this.argNodeList = new ArrayList<>();
			for(Node node : args.nodeList) {
				argNodeList.add((ExprNode) this.setNodeAsChild(node));
			}
		}

		public String getClassName() {
			return this.className;
		}

		public List<ExprNode> getNodeList() {
			return this.argNodeList;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents conditional 'and' or 'or' operator.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class CondOpNode extends ExprNode {
		private final String condOp;
		private final ExprNode leftNode;
		private final ExprNode rightNode;

		public CondOpNode(Token token, Node leftNode, Node rightNode) {
			this.setToken(token);
			this.condOp = this.token.getText();
			this.leftNode = (ExprNode) this.setNodeAsChild(leftNode);
			this.rightNode = (ExprNode) this.setNodeAsChild(rightNode);
		}

		public String getConditionalOp() {
			return this.condOp;
		}

		public ExprNode getLeftNode() {
			return this.leftNode;
		}

		public ExprNode getRightNode() {
			return this.rightNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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
		private final ExprNode exprNode;

		public AssertNode(Token token, Node exprNode) {
			this.setToken(token);
			this.exprNode = (ExprNode) this.setNodeAsChild(exprNode);
		}

		public Node getExprNode() {
			return this.exprNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents block.
	 * It contains several statements.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class BlockNode extends Node {
		private final List<Node> nodeList;

		public BlockNode(Block block) {
			this.nodeList = new ArrayList<>();
			for(Node node : block.getNodeList()) {
				this.nodeList.add(this.setNodeAsChild(node));
			}
		}

		public List<Node> getNodeList() {
			return this.nodeList;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents exporting variable as environmental variable.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ExportEnvNode extends Node {
		private final String envName;
		private final ExprNode exprNode;

		public ExportEnvNode(Token token, Node exprNode) {
			this.setToken(token);
			this.envName = this.token.getText();
			this.exprNode = (ExprNode) this.setNodeAsChild(exprNode);
		}

		public String getEnvName() {
			return this.envName;
		}

		public ExprNode getExprNode() {
			return this.exprNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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
		private final ExprNode condNode;

		/**
		 * May be EmptyNode
		 */
		private final Node iterNode;
		private final BlockNode blockNode;

		public ForNode(Token token, Node initNode, Node condNode, Node iterNode, Node blockNode) {
			this.setToken(token);
			this.initNode = this.setNodeAsChild(initNode);
			this.condNode = (ExprNode) this.setNodeAsChild(condNode);
			this.iterNode = this.setNodeAsChild(iterNode);
			this.blockNode = (BlockNode) this.setNodeAsChild(blockNode);
		}

		public Node getInitNode() {
			return this.initNode;
		}

		public ExprNode getCondNode() {
			return this.condNode;
		}

		public Node getIterNode() {
			return this.iterNode;
		}

		public BlockNode getBlockNode() {
			return this.blockNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents for in statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ForInNode extends Node {
		private final String initName;
		private final ExprNode exprNode;
		private final BlockNode blockNode;

		public ForInNode(Token token, Token nameToken, Node exprNode, Node blockNode) {
			this.setToken(token);
			this.initName = nameToken.getText();
			this.exprNode = (ExprNode) this.setNodeAsChild(exprNode);
			this.blockNode = (BlockNode) this.setNodeAsChild(blockNode);
		}

		public String getInitName() {
			return this.initName;
		}

		public ExprNode getExprNode() {
			return this.exprNode;
		}

		public BlockNode getBlockNode() {
			return this.blockNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents while statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class WhileNode extends Node {
		private final ExprNode condNode;
		private final BlockNode blockNode;

		public WhileNode(Token token, Node condNode, Node blockNode) {
			this.setToken(token);
			this.condNode = (ExprNode) this.setNodeAsChild(condNode);
			this.blockNode = (BlockNode) this.setNodeAsChild(blockNode);
		}

		public ExprNode getCondNode() {
			return this.condNode;
		}

		public BlockNode getBlockNode() {
			return this.blockNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents if statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class IfNode extends Node {
		private final ExprNode condNode;
		private final BlockNode thenBlockNode;
		/**
		 * May be EmptyblockNode
		 */
		private final BlockNode elseBlockNode;

		public IfNode(Token token, Node condNode, IfElseBlock block) {
			this.setToken(token);
			this.condNode = (ExprNode) this.setNodeAsChild(condNode);
			this.thenBlockNode = (BlockNode) this.setNodeAsChild(block.getThenBlockNode());
			this.elseBlockNode = (BlockNode) this.setNodeAsChild(block.getElseBlockNode());
		}

		public ExprNode getCondNode() {
			return this.condNode;
		}

		public BlockNode getThenBlockNode() {
			return this.thenBlockNode;
		}

		public BlockNode getElseBlockNode() {
			return this.elseBlockNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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
		private final ExprNode exprNode;

		public ReturnNode(Token token, ReturnExpr returnExpr) {
			this.setToken(token);
			this.exprNode = (ExprNode) this.setNodeAsChild(returnExpr.getExprNode());
		}

		public ExprNode getExprNode() {
			return this.exprNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents throw statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ThrowNode extends Node {
		private final ExprNode exprNode;

		public ThrowNode(Token token, Node exprNode) {
			this.setToken(token);
			this.exprNode = (ExprNode) this.setNodeAsChild(exprNode);
		}

		public ExprNode getExprNode() {
			return this.exprNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents try-catch statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class TryNode extends Node {
		private final BlockNode tryBlockNode;
		private final List<CatchNode> catchNodeList;
		/**
		 * May be EmptyBlockNode.
		 */
		private final BlockNode finallyBlockNode;

		public TryNode(Token token, Node tryBlockNode, Node finallyBlockNode) {
			this.setToken(token);
			this.tryBlockNode = (BlockNode) this.setNodeAsChild(tryBlockNode);
			this.catchNodeList = new ArrayList<>();
			this.finallyBlockNode = (BlockNode) this.setNodeAsChild(finallyBlockNode);
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
			this.catchNodeList.add((CatchNode) this.setNodeAsChild(node));
		}

		public List<CatchNode> getCatchNodeList() {
			return this.catchNodeList;
		}

		public BlockNode getFinallyBlockNode() {
			return this.finallyBlockNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents catch statement.
	 * It always exists in TryNode.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class CatchNode extends Node {
		private ClassType exceptionType;
		private final String exceptionName;
		private final TypeSymbol exceptionTypeSymbol;
		private final BlockNode catchBlockNode;

		public CatchNode(Token token, String exceptionName, TypeSymbol typeSymbol, Node catchBlockNode) {
			this.setToken(token);
			this.exceptionName = exceptionName;
			this.exceptionTypeSymbol = typeSymbol;
			this.catchBlockNode = (BlockNode) this.setNodeAsChild(catchBlockNode);
		}

		/**
		 * 
		 * @return
		 * - return null, if has no type annotation.
		 */
		public TypeSymbol getTypeSymbol() {
			return this.exceptionTypeSymbol;
		}

		public void setExceptionType(ClassType exceptionType) {
			this.exceptionType = exceptionType;
		}

		public ClassType getExceptionType() {
			return this.exceptionType;
		}

		public String getExceptionVarName() {
			return this.exceptionName;
		}

		public BlockNode getCatchBlockNode() {
			return this.catchBlockNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents variable declaration.
	 * It requires initial value.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class VarDeclNode extends Node {
		private final boolean isReadOnly;
		private boolean isGlobal;
		private final String varName;
		private final ExprNode initValueNode;

		public VarDeclNode(Token token, Token nameToken, Node initValueNode) {
			this.setToken(token);
			this.varName = nameToken.getText();
			this.initValueNode = (ExprNode) this.setNodeAsChild(initValueNode);
			this.isReadOnly = !token.getText().equals("var");
			this.isGlobal = false;
		}

		public String getVarName() {
			return this.varName;
		}

		public ExprNode getInitValueNode() {
			return this.initValueNode;
		}

		public void setGlobal(boolean isGlobal) {
			this.isGlobal = isGlobal;
		}

		public boolean isGlobal() {
			return this.isGlobal;
		}

		public boolean isReadOnly() {
			return this.isReadOnly;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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
		private final AssignableNode leftNode;
		private final ExprNode rightNode;

		public AssignNode(Token token, Node leftNode, Node rightNode) {
			this.setToken(token);
			this.assignOp = this.token.getText();
			this.leftNode = (AssignableNode) this.setNodeAsChild(leftNode);
			this.rightNode = (ExprNode) this.setNodeAsChild(rightNode);
		}

		public String getAssignOp() {
			return this.assignOp;
		}

		public AssignableNode getLeftNode() {
			return this.leftNode;
		}

		public ExprNode getRightNode() {
			return this.rightNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * This node represents function.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FunctionNode extends Node {
		private final String funcName;
		private final List<SymbolNode> nodeList;
		private final BlockNode blockNode;

		public FunctionNode(Token token, Token nameToken, ArgsDecl decl, Node blockNode) {
			this.setToken(token);
			this.funcName = nameToken.getText();
			this.blockNode = (BlockNode) this.setNodeAsChild(blockNode);
			this.nodeList = new ArrayList<>();
			for(Node node : decl.getNodeList()) {
				this.nodeList.add((SymbolNode) this.setNodeAsChild(node));
			}
		}

		public String getFuncName() {
			return this.funcName;
		}

		public List<SymbolNode> getArgDeclNodeList() {
			return this.nodeList;
		}

		public BlockNode getBlockNode() {
			return this.blockNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
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
		/**
		 * may be null, if not super class.
		 */
		private final String superName;
		private ClassType classType;
		private final List<Node> classElementList;

		/**
		 * 
		 * @param token
		 * @param nameToken
		 * - class name token
		 * @param superName
		 * - super class name (may be null if had no super class).
		 * @param elementList
		 * - class elements
		 */
		public ClassNode(Token token, Token nameToken, String superName, List<Node> elementList) {
			this.setToken(token);
			this.className = nameToken.getText();
			this.superName = superName;
			this.classElementList = new ArrayList<>();
			for(Node node : elementList) {
				this.classElementList.add(this.setNodeAsChild(node));
			}
		}

		public String getClassName() {
			return this.className;
		}

		/**
		 * 
		 * @return
		 * - return null. if has no super class.
		 */
		public String getSuperName() {
			return this.superName;
		}

		public void setClassType(ClassType type) {
			this.classType = type;
		}

		public ClassType getClassType() {
			return this.classType;
		}

		public List<Node> getElementList() {
			return this.classElementList;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * Represents class constructor.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ConstructorNode extends Node {
		private Type recvType;
		private final List<SymbolNode> nodeList;
		private final BlockNode blockNode;

		public ConstructorNode(Token token, ArgsDecl decl, Node blockNode) {
			this.setToken(token);
			this.recvType = TypePool.unresolvedType;
			this.blockNode = (BlockNode) this.setNodeAsChild(blockNode);
			this.nodeList = new ArrayList<>();
			for(Node node : decl.getNodeList()) {
				this.nodeList.add((SymbolNode) this.setNodeAsChild(node));
			}
		}

		public void setRecvType(Type type) {
			this.recvType = type;
		}

		public Type getRecvType() {
			return this.recvType;
		}

		public List<SymbolNode> getArgDeclNodeList() {
			return this.nodeList;
		}

		public BlockNode getBlockNode() {
			return this.blockNode;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * Represents empty statement.
	 * It is ignored in type checking or code generation.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class EmptyNode extends Node {

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
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

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * Represent root node.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class RootNode extends Node {
		private final List<Node> nodeList;

		public RootNode() {
			this.nodeList = new ArrayList<>();
		}

		public void addNode(Node node) {
			this.nodeList.add(this.setNodeAsChild(node));
		}

		public List<Node> getNodeList() {
			return this.nodeList;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}
}
