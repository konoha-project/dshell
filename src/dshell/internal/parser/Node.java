package dshell.internal.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import dshell.internal.parser.CalleeHandle.ConstructorHandle;
import dshell.internal.parser.CalleeHandle.FieldHandle;
import dshell.internal.parser.CalleeHandle.MethodHandle;
import dshell.internal.parser.CalleeHandle.OperatorHandle;
import dshell.internal.parser.CalleeHandle.StaticFieldHandle;
import dshell.internal.parser.ParserUtils.ArgDecl;
import dshell.internal.parser.ParserUtils.ArgsDecl;
import dshell.internal.parser.ParserUtils.Arguments;
import dshell.internal.parser.ParserUtils.Block;
import dshell.internal.parser.ParserUtils.IfElseBlock;
import dshell.internal.parser.TypePool.ClassType;
import dshell.internal.parser.TypePool.FuncHolderType;
import dshell.internal.parser.TypePool.Type;

/**
 * Represents dshell grammar element.
 * @author skgchxngsxyz-osx
 *
 */
public abstract class Node {
	/**
	 * for line number generation.
	 * may be null.
	 */
	protected final Token token;

	protected Node parentNode;

	protected Node(Token token) {
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
	 * set expr node as this node's child
	 * @param childNode
	 * @return
	 * - child node
	 */
	public ExprNode setExprNodeAsChild(ExprNode childNode) {
		return (ExprNode) this.setNodeAsChild(childNode);
	}

	/**
	 * get parent node.
	 * @return
	 * - return null, if this node is RootNode.
	 */
	public Node getParentNode() {
		return this.parentNode;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ":voidType";
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
		 * It represents expression return type.
		 */
		protected Type type = TypePool.unresolvedType;

		protected ExprNode(Token token) {
			super(token);
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

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + ":" + this.type;
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
			super(token);
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
			super(token);
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
			super(token);
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
			super(token);
			this.value = parseTokenText(this.token);
		}

		/**
		 * used for CommandNode
		 * @param value
		 */
		public StringValueNode(String value) {
			super(null);
			this.value = value;
		}
		public static String parseTokenText(Token token) {
			StringBuilder sBuilder = new StringBuilder();
			String text = token.getText();
			int startIndex = 1;
			int endIndex = text.length() - 1;
			if(!text.startsWith("\"")) {	// for command argument. FIXME:
				startIndex = 0;
				endIndex += 1;
			}
			for(int i = startIndex; i < endIndex; i++) {
				char ch = text.charAt(i);
				if(ch == '\\') {
					char nextCh = text.charAt(++i);
					switch(nextCh) {
					case 't' : ch = '\t'; break;
					case 'b' : ch = '\b'; break;
					case 'n' : ch = '\n'; break;
					case 'r' : ch = '\r'; break;
					case 'f' : ch = '\f'; break;
					case '\'': ch = '\''; break;
					case '"' : ch = '"';  break;
					case '\\': ch = '\\'; break;
					}
				}
				sBuilder.append(ch);
			}
			return sBuilder.toString();
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
			super(token);
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
			super(null);
			this.nodeList = new ArrayList<>();
		}

		public void addNode(ExprNode node) {
			this.nodeList.add(this.setExprNodeAsChild(node));
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
			super(null);
			this.keyList = new ArrayList<>();
			this.valueList = new ArrayList<>();
		}

		public void addEntry(ExprNode keyNode, ExprNode valueNode) {
			this.keyList.add(this.setExprNodeAsChild(keyNode));
			this.valueList.add(this.setExprNodeAsChild(valueNode));
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
		protected boolean isReadOnly;

		protected AssignableNode(Token token) {
			super(token);
		}

		public void setReadOnly(boolean isReadOnly) {
			this.isReadOnly = isReadOnly;
		}

		public boolean isReadOnly() {
			return this.isReadOnly;
		}
	}

	/**
	 * This node represents local variable.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class SymbolNode extends AssignableNode {
		private final String symbolName;

		/**
		 * used for getting function object from static field.
		 */
		private StaticFieldHandle handle;

		public SymbolNode(Token token) {
			super(token);
			this.symbolName = this.token.getText();
		}

		public String getSymbolName() {
			return this.symbolName;
		}

		public void setHandle(StaticFieldHandle handle) {
			this.handle = handle;
		}

		public StaticFieldHandle getHandle() {
			return this.handle;
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

		public ElementGetterNode(ExprNode recvNode, ExprNode indexNode) {
			super(null);
			this.recvNode = this.setExprNodeAsChild(recvNode);
			this.indexNode = this.setExprNodeAsChild(indexNode);
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

		public FieldGetterNode(ExprNode recvNode, Token token) {
			super(token);
			this.recvNode = this.setExprNodeAsChild(recvNode);
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

		public CastNode(TypeSymbol targetTypeSymbol, ExprNode exprNode) {
			super(null);
			this.targetTypeSymbol = targetTypeSymbol;
			this.exprNode = this.setExprNodeAsChild(exprNode);
		}

		/**
		 * called from type checker.
		 * @param targetType
		 * @param exprNode
		 */
		public CastNode(Type targetType, ExprNode exprNode) {
			super(null);
			this.targetTypeSymbol = null; //FIXME:
			this.exprNode = this.setExprNodeAsChild(exprNode);
			this.targetType = targetType;
			this.type = targetType;
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

		public InstanceofNode(Token token, ExprNode exprNode, TypeSymbol targetTypeSymbol) {
			super(token);
			this.exprNode = this.setExprNodeAsChild(exprNode);
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
	 * Represnts operator( binary op, unary op) call.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class OperatorCallNode extends ExprNode {
		private final String funcName;
		private final List<ExprNode> argNodeList;
		private OperatorHandle handle;

		/**
		 * For prefix op
		 * @param token
		 * - operator token
		 * @param node
		 * - operand
		 */
		public OperatorCallNode(Token token, ExprNode node) {
			super(token);
			this.funcName = this.token.getText();
			this.argNodeList = new ArrayList<>();
			this.argNodeList.add(this.setExprNodeAsChild(node));
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
		public OperatorCallNode(Token token, ExprNode leftNode, ExprNode rightNode) {
			super(token);
			this.funcName = this.token.getText();
			this.argNodeList = new ArrayList<>();
			this.argNodeList.add(this.setExprNodeAsChild(leftNode));
			this.argNodeList.add(this.setExprNodeAsChild(rightNode));
		}

		public String getFuncName() {
			return this.funcName;
		}

		public List<ExprNode> getNodeList() {
			return this.argNodeList;
		}

		public void setHandle(OperatorHandle handle) {
			this.handle = handle;
		}

		public OperatorHandle getHandle() {
			return this.handle;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	public static class ApplyNode extends ExprNode {
		protected final ExprNode recvNode;
		protected final List<ExprNode> argList;

		/**
		 * if true, treat this as function call.
		 */
		protected boolean isFuncCall;
		protected MethodHandle handle;

		protected ApplyNode(ExprNode recvNode, Arguments args) {
			super(recvNode.getToken());
			this.recvNode = this.setExprNodeAsChild(recvNode);
			this.argList = new ArrayList<>();
			for(ExprNode argNode : args.nodeList) {
				this.argList.add(this.setExprNodeAsChild(argNode));
			}
			this.isFuncCall = false;
		}

		public ExprNode getRecvNode() {
			return this.recvNode;
		}

		public List<ExprNode> getArgList() {
			return this.argList;
		}

		public void setAsFuncCallNode() {
			this.isFuncCall = true;
		}

		public boolean isFuncCall() {
			return this.isFuncCall;
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
		private final TypeSymbol typeSymbol;
		private final List<ExprNode> argNodeList;
		private ConstructorHandle handle;

		public ConstructorCallNode(Token token, TypeSymbol typeSymbol, Arguments args) {
			super(token);
			this.typeSymbol = typeSymbol;
			this.argNodeList = new ArrayList<>();
			for(ExprNode node : args.nodeList) {
				argNodeList.add(this.setExprNodeAsChild(node));
			}
		}

		public TypeSymbol getTypeSymbol() {
			return this.typeSymbol;
		}

		public List<ExprNode> getNodeList() {
			return this.argNodeList;
		}

		public void setHandle(ConstructorHandle handle) {
			this.handle = handle;
		}

		public ConstructorHandle getHandle() {
			return this.handle;
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

		public CondOpNode(Token token, ExprNode leftNode, ExprNode rightNode) {
			super(token);
			this.condOp = this.token.getText();
			this.leftNode = this.setExprNodeAsChild(leftNode);
			this.rightNode = this.setExprNodeAsChild(rightNode);
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

	public static class CommandNode extends ExprNode {
		private final String commandPath;
		private final List<ExprNode> argNodeList;

		protected CommandNode(Token token, String commandPath) {
			super(token);
			this.argNodeList = new ArrayList<>();
			this.commandPath = commandPath;
		}

		public void setArg(ExprNode argNode) {
			this.argNodeList.add(this.setExprNodeAsChild(argNode));
		}

		public String getCommandPath() {
			return this.commandPath;
		}

		public List<ExprNode> getArgNodeList() {
			return this.argNodeList;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}
	
	
	
	// #################
	// #   statement   #
	// #################
	/**
	 * This node represents assertion.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class AssertNode extends Node {
		public final static String opName = "assert";
		private final ExprNode exprNode;
		private OperatorHandle handle;

		public AssertNode(Token token, ExprNode exprNode) {
			super(token);
			this.exprNode = this.setExprNodeAsChild(exprNode);
		}

		public Node getExprNode() {
			return this.exprNode;
		}

		public void setHandle(OperatorHandle handle) {
			this.handle = handle;
		}

		public OperatorHandle getHandle() {
			return this.handle;
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
			super(null);
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
	 * represent end of block stattement (break, continue, return throw)
	 * @author skgchxngsxyz-opensuse
	 *
	 */
	public abstract static class BlockEndNode extends Node {
		protected BlockEndNode(Token token) {
			super(token);
		}
	}

	/**
	 * This node represents break statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class BreakNode extends BlockEndNode {
		public BreakNode(Token token) {
			super(token);
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
	public static class ContinueNode extends BlockEndNode {
		public ContinueNode(Token token) {
			super(token);
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
		private OperatorHandle handle;

		public ExportEnvNode(Token token, Token nameToken, ExprNode exprNode) {
			super(token);
			this.envName = nameToken.getText();
			this.exprNode = this.setExprNodeAsChild(exprNode);
		}

		public String getEnvName() {
			return this.envName;
		}

		public ExprNode getExprNode() {
			return this.exprNode;
		}

		public void setHandle(OperatorHandle handle) {
			this.handle = handle;
		}

		public OperatorHandle getHandle() {
			return this.handle;
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
		private OperatorHandle handle;

		public ImportEnvNode(Token token) {
			super(token);
			this.envName = this.token.getText();
		}

		public String getEnvName() {
			return this.envName;
		}

		public void setHandle(OperatorHandle handle) {
			this.handle = handle;
		}

		public OperatorHandle getHandle() {
			return this.handle;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	/**
	 * represent loop statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static abstract class LoopNode extends Node {
		protected LoopNode(Token token) {
			super(token);
		}
	}

	/**
	 * This node represents for statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ForNode extends LoopNode {
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

		public ForNode(Token token, Node initNode, ExprNode condNode, Node iterNode, Node blockNode) {
			super(token);
			this.initNode = this.setNodeAsChild(initNode);
			this.condNode = this.setExprNodeAsChild(condNode);
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
	public static class ForInNode extends LoopNode {
		private final String initName;
		private final ExprNode exprNode;
		private final BlockNode blockNode;

		public ForInNode(Token token, Token nameToken, ExprNode exprNode, Node blockNode) {
			super(token);
			this.initName = nameToken.getText();
			this.exprNode = this.setExprNodeAsChild(exprNode);
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
	public static class WhileNode extends LoopNode {
		private final ExprNode condNode;
		private final BlockNode blockNode;

		public WhileNode(Token token, ExprNode condNode, Node blockNode) {
			super(token);
			this.condNode = this.setExprNodeAsChild(condNode);
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

		public IfNode(Token token, ExprNode condNode, IfElseBlock block) {
			super(token);
			this.condNode = this.setExprNodeAsChild(condNode);
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
	public static class ReturnNode extends BlockEndNode {
		/**
		 * May be EmptyNode.
		 */
		private final ExprNode exprNode;

		public ReturnNode(Token token, ExprNode exprNode) {
			super(token);
			this.exprNode = this.setExprNodeAsChild(exprNode);
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
	public static class ThrowNode extends BlockEndNode {
		private final ExprNode exprNode;

		public ThrowNode(Token token, ExprNode exprNode) {
			super(token);
			this.exprNode = this.setExprNodeAsChild(exprNode);
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
			super(token);
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
			super(token);
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

		public VarDeclNode(Token token, Token nameToken, ExprNode initValueNode) {
			super(token);
			this.varName = nameToken.getText();
			this.initValueNode = this.setExprNodeAsChild(initValueNode);
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
		 * if assingOp is '=', it is null.
		 */
		private OperatorHandle handle;

		/**
		 * requires SymbolNode, ElementGetterNode or FieldGetterNode.
		 */
		private final ExprNode leftNode;
		private final ExprNode rightNode;

		public AssignNode(Token token, ExprNode leftNode, ExprNode rightNode) {
			super(token);
			this.assignOp = this.token.getText();
			this.leftNode = this.setExprNodeAsChild(leftNode);
			this.rightNode = this.setExprNodeAsChild(rightNode);
		}

		public String getAssignOp() {
			return this.assignOp;
		}

		public ExprNode getLeftNode() {
			return this.leftNode;
		}

		public ExprNode getRightNode() {
			return this.rightNode;
		}

		public void setHandle(OperatorHandle handle) {
			this.handle = handle;
		}

		/**
		 * 
		 * @return
		 * return null, if operator is '='.
		 */
		public OperatorHandle getHandle() {
			return this.handle;
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
	public static class SuffixIncrementNode extends Node {
		private final ExprNode leftNode;
		/**
		 * ++ or --
		 */
		private final String op;

		private OperatorHandle handle;

		public SuffixIncrementNode(ExprNode exprNode, Token token) {
			super(token);
			this.leftNode = this.setExprNodeAsChild(exprNode);
			this.op = this.token.getText();
		}

		public ExprNode getLeftNode() {
			return this.leftNode;
		}

		public String getOperator() {
			return this.op;
		}

		public void setHandle(OperatorHandle handle) {
			this.handle = handle;
		}

		public OperatorHandle getHandle() {
			return this.handle;
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
		private final List<TypeSymbol> paramTypeSymbolList;
		private final List<SymbolNode> nodeList;
		private final BlockNode blockNode;
		private final TypeSymbol returnTypeSymbol;
		private FuncHolderType holderType;

		public FunctionNode(Token token, Token nameToken, TypeSymbol returnTypeSymbol, ArgsDecl decls, Node blockNode) {
			super(token);
			this.funcName = nameToken.getText();
			this.returnTypeSymbol = returnTypeSymbol;
			this.blockNode = (BlockNode) this.setNodeAsChild(blockNode);
			this.paramTypeSymbolList = new ArrayList<>();
			this.nodeList = new ArrayList<>();
			for(ArgDecl decl : decls.getDeclList()) {
				this.paramTypeSymbolList.add(decl.getTypeSymbol());
				this.nodeList.add((SymbolNode) this.setNodeAsChild(decl.getArgNode()));
			}
		}

		public String getFuncName() {
			return this.funcName;
		}

		public List<SymbolNode> getArgDeclNodeList() {
			return this.nodeList;
		}

		public List<TypeSymbol> getParamTypeSymbolList() {
			return this.paramTypeSymbolList;
		}

		public BlockNode getBlockNode() {
			return this.blockNode;
		}

		public TypeSymbol getRetunrTypeSymbol() {
			return this.returnTypeSymbol;
		}

		public void setHolderType(FuncHolderType holderType) {
			this.holderType = holderType;
		}

		public FuncHolderType getHolderType() {
			return this.holderType;
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
			super(token);
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
		private final List<TypeSymbol> typeSymbolList;
		private final List<SymbolNode> nodeList;
		private final BlockNode blockNode;

		public ConstructorNode(Token token, ArgsDecl decls, Node blockNode) {
			super(token);
			this.recvType = TypePool.unresolvedType;
			this.blockNode = (BlockNode) this.setNodeAsChild(blockNode);
			this.typeSymbolList = new ArrayList<>();
			this.nodeList = new ArrayList<>();
			for(ArgDecl decl : decls.getDeclList()) {
				this.typeSymbolList.add(decl.getTypeSymbol());
				this.nodeList.add((SymbolNode) this.setNodeAsChild(decl.getArgNode()));
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
	 * Represents empty expression. used for for statement and return statement.
	 * it is always void type
	 * It is ignored in code generation.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class EmptyNode extends ExprNode {
		public EmptyNode() {
			super(null);
		}

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
		public final static EmptyBlockNode INSTANCE = new EmptyBlockNode();

		private EmptyBlockNode() {
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
		public final static String opName = "printValue";
		private final List<Node> nodeList;

		/**
		 * used for interactive mode.
		 */
		private OperatorHandle handle;

		public RootNode(Token token) {
			super(token);
			this.nodeList = new ArrayList<>();
		}

		public void addNode(Node node) {
			this.nodeList.add(this.setNodeAsChild(node));
		}

		public List<Node> getNodeList() {
			return this.nodeList;
		}

		public void setHandle(OperatorHandle handle) {
			this.handle = handle;
		}

		public OperatorHandle getHandle() {
			return this.handle;
		}

		@Override
		public <T> T accept(NodeVisitor<T> visitor) { // do not call it
			throw new RuntimeException("RootNode do not support NodeVisitor");
		}
	}
}
