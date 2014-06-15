package dshell.internal.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import dshell.internal.parser.Node.BlockNode;
import dshell.internal.parser.Node.ExprNode;
import dshell.internal.parser.Node.SymbolNode;
import dshell.internal.parser.TypeSymbol.VoidTypeSymbol;

public class ParserUtils {
	public static class MapEntry {
		public final Node keyNode;
		public final Node valueNode;

		public MapEntry(Node keyNode, Node valueNode) {
			this.keyNode = keyNode;
			this.valueNode = valueNode;
		}
	}

	public static class Arguments {
		public final ArrayList<ExprNode> nodeList;

		public Arguments() {
			this.nodeList = new ArrayList<>();
		}

		public void addNode(Node node) {
			this.nodeList.add((ExprNode) node);
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
		private ExprNode exprNode;

		public ReturnExpr() {
			this.exprNode = new Node.EmptyNode();
		}

		public void setNode(Node node) {
			this.exprNode = (ExprNode) node;
		}

		public ExprNode getExprNode() {
			return this.exprNode;
		}
	}

	public static class CatchedException {
		private TypeSymbol typeSymbol;
		private final String name;

		public CatchedException(Token token) {
			this.name = token.getText();
			this.typeSymbol = null;
		}

		public void setTypeSymbol(TypeSymbol typeSymbol) {
			this.typeSymbol = typeSymbol;
		}

		public String getName() {
			return this.name;
		}

		/**
		 * 
		 * @return
		 * - return null, if has no type annotation
		 */
		public TypeSymbol getTypeSymbol() {
			return this.typeSymbol;
		}
	}

	public static class ArgsDecl {
		private final List<ArgDecl> declList;

		public ArgsDecl() {
			this.declList = new ArrayList<>();
		}

		public List<ArgDecl> getDeclList() {
			return this.declList;
		}

		public void addArgDecl(ArgDecl decl) {
			this.declList.add(decl);
		}
	}

	public static class ArgDecl {
		private final SymbolNode argDeclNode;
		private final TypeSymbol typeSymbol;

		public ArgDecl(Token token, TypeSymbol typeSymbol) {
			this.argDeclNode = new SymbolNode(token);
			this.typeSymbol = typeSymbol;
		}

		public SymbolNode getArgNode() {
			return this.argDeclNode;
		}

		public TypeSymbol getTypeSymbol() {
			return this.typeSymbol;
		}
	}

	public static class ClassBody {
		private final List<Node> nodeList;

		public ClassBody() {
			this.nodeList = new ArrayList<>();
		}

		public void addNode(Node node) {
			this.nodeList.add(node);
		}

		public List<Node> getNodeList() {
			return this.nodeList;
		}
	}

	public static class ParamTypeResolver {
		private final List<TypeSymbol> symbolList;

		public ParamTypeResolver() {
			this.symbolList = new ArrayList<>();
		}

		public void addTypeSymbol(TypeSymbol typeSymbol) {
			if(!(typeSymbol instanceof VoidTypeSymbol)) {
				this.symbolList.add(typeSymbol);
			}
		}

		public TypeSymbol[] getTypeSymbols() {
			int size = this.symbolList.size();
			TypeSymbol[] symbols = new TypeSymbol[size];
			for(int i = 0; i < size; i++) {
				symbols[i] = this.symbolList.get(i);
			}
			return symbols;
		}
	}
}
