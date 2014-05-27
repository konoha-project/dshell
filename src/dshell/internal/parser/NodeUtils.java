package dshell.internal.parser;

import java.util.ArrayList;

import org.antlr.v4.runtime.Token;

import dshell.internal.parser.Node.BlockNode;
import dshell.internal.parser.TypePool.Type;

public class NodeUtils {
	public static class MapEntry {
		public final Node keyNode;
		public final Node valueNode;

		public MapEntry(Node keyNode, Node valueNode) {
			this.keyNode = keyNode;
			this.valueNode = valueNode;
		}
	}

	public static class Arguments {
		public final ArrayList<Node> nodeList;

		public Arguments() {
			this.nodeList = new ArrayList<>();
		}

		public void addNode(Node node) {
			this.nodeList.add(node);
		}
	}

	public static class Block {
		private final ArrayList<Node> nodeList;

		public Block() {
			this.nodeList = new ArrayList<>();
		}

		public ArrayList<Node> getNodeList() {
			return this.nodeList;
		}

		public void addNode(Node node) {
			this.nodeList.add(node);
		}
	}

	public static class IfElseBlock {
		private BlockNode thenBlockNode;
		private BlockNode elseBlockNode;

		public IfElseBlock(Node node) {
			this.thenBlockNode = (BlockNode) node;
			this.elseBlockNode = new Node.EmptyBlockNode();
		}

		public void setElseBlockNode(Node node) {
			this.elseBlockNode = (BlockNode) node;
		}

		public BlockNode getThenBlockNode() {
			return this.thenBlockNode;
		}

		public BlockNode getElseBlockNode() {
			return this.elseBlockNode;
		}
	}

	public static class ReturnExpr {
		private Node exprNode;

		public ReturnExpr() {
			this.exprNode = new Node.EmptyNode();
		}

		public void setNode(Node node) {
			this.exprNode = node;
		}

		public Node getExprNode() {
			return this.exprNode;
		}
	}

	public static class CatchedException {
		private Type type;
		private String name;

		public CatchedException(Token token) {
			this.name = token.getText();
			this.type = TypePool.getInstance().exceptionType;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public String getName() {
			return this.name;
		}

		public Type getType() {
			return this.type;
		}
	}
}
