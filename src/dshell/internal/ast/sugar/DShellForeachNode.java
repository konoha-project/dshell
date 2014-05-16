package dshell.internal.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.binary.AssignNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.binary.BunLessThanNode;
import libbun.ast.binary.ComparatorNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.expression.GetIndexNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.expression.MethodCallNode;
import libbun.ast.literal.BunBooleanNode;
import libbun.ast.literal.BunIntNode;
import libbun.ast.statement.BunIfNode;
import libbun.parser.classic.LibBunTypeChecker;
import libbun.type.BType;
import dshell.internal.ast.DShellForNode;
import dshell.internal.lib.Utils;

/**
for(value in Expr) {
  Block
}

==>
if(true) {
  var values = Expr
  var size = values.Size()
  for(var index = 0; index < size; index = index + 1) {
    var value = values[index]
    Block
  }
}
**/
public class DShellForeachNode extends SyntaxSugarNode {
	public final static int _Value = 0;
	public final static int _Expr  = 1;
	public final static int _Block = 2;

	public DShellForeachNode(BNode parentNode) {
		super(parentNode, 3);
	}

	@Override
	public void PerformTyping(LibBunTypeChecker typeChecker, BType contextType) {
		typeChecker.CheckTypeAt(this, _Expr, BType.VarType);
		if(!this.AST[_Expr].Type.IsArrayType()) {
			this.SetNode(_Expr, new ErrorNode(this.ParentNode, this.SourceToken, "require array type"));
		}
	}

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker typeChecker) {
		String valuesSymbol = typeChecker.Generator.NameUniqueSymbol("values");
		String sizeSymbol = typeChecker.Generator.NameUniqueSymbol("size");
		String indexSymbol = typeChecker.Generator.NameUniqueSymbol("index");
		// create if
		BNode node = new BunIfNode(this.ParentNode);
		node.SetNode(BunIfNode._Cond, new BunBooleanNode(true));
		BunBlockNode thenBlockNode = new BunBlockNode(node, null);
		node.SetNode(BunIfNode._Then, thenBlockNode);
		// create var
		this.createAndSetValueDeclNode(thenBlockNode, valuesSymbol, this.AST[_Expr]);
		this.createAndSetSizeDeclNode(thenBlockNode, sizeSymbol, valuesSymbol);
		// create for
		DShellForNode forNode = new DShellForNode(thenBlockNode);
		thenBlockNode.SetNode(BNode._AppendIndex, forNode);
		forNode.SetNode(DShellForNode._Init, this.createIndexDeclNode(forNode, indexSymbol));
		forNode.SetNode(DShellForNode._Cond, this.createCondNode(forNode, indexSymbol, sizeSymbol));
		forNode.SetNode(DShellForNode._Next, this.createIncrementNode(forNode, indexSymbol));
		forNode.SetNode(DShellForNode._Block, this.createForBlockNode(forNode, valuesSymbol, indexSymbol));
		return new DesugarNode(this, node);
	}

	private void createAndSetValueDeclNode(BunBlockNode parentNode, String valuesSymbol, BNode exprNode) {
		BunLetVarNode node = new BunLetVarNode(parentNode, BunLetVarNode._IsReadOnly, null, valuesSymbol);
		node.SetNode(BunLetVarNode._InitValue, exprNode);
		parentNode.SetNode(BNode._AppendIndex, node);
	}

	private void createAndSetSizeDeclNode(BunBlockNode parentNode, String sizeSymbol, String valuesSymbol) {
		BunLetVarNode node = new BunLetVarNode(parentNode, BunLetVarNode._IsReadOnly, null, sizeSymbol);
		node.SourceToken = this.SourceToken;
		MethodCallNode sizeNode = new MethodCallNode(parentNode, new GetNameNode(parentNode, null, valuesSymbol), "Size");
		sizeNode.SourceToken = this.SourceToken; // for line number
		node.SetNode(BunLetVarNode._InitValue, sizeNode);
		parentNode.SetNode(BNode._AppendIndex, node);
	}

	private BunLetVarNode createIndexDeclNode(BNode parentNode, String indexSymbol) {
		BunLetVarNode node = new BunLetVarNode(parentNode, 0, null, indexSymbol);
		node.SetNode(BunLetVarNode._InitValue, new BunIntNode(node, null, 0));
		return node;
	}

	private ComparatorNode createCondNode(BNode parentNode, String indexSymbol, String sizeSymbol) {
		BNode leftNode = new GetNameNode(parentNode, null, indexSymbol);
		BNode rightNode = new GetNameNode(parentNode, null, sizeSymbol);
		BunLessThanNode node = new BunLessThanNode(parentNode);
		node.SetLeftNode(leftNode);
		node.SetRightNode(rightNode);
		return node;
	}

	private AssignNode createIncrementNode(BNode parentNode, String indexSymbol) {
		BinaryOperatorNode binaryNode = new BunAddNode(parentNode);
		binaryNode.SetLeftNode(new GetNameNode(binaryNode, null, indexSymbol));
		binaryNode.SetRightNode(new BunIntNode(binaryNode, null, 1));
		return new AssignNode(indexSymbol, binaryNode);
	}

	private BunLetVarNode createValueDeclNode(BNode parentNode, String valuesSymbol, String indexSymbol) {
		BNode indexNode = new GetIndexNode(parentNode, new GetNameNode(parentNode, null, valuesSymbol));
		indexNode.SetNode(GetIndexNode._Index, new GetNameNode(indexNode, null, indexSymbol));
		BunLetVarNode node = new BunLetVarNode(parentNode, 0, null, this.getName());
		node.SetNode(BunLetVarNode._InitValue, indexNode);
		return node;
	}

	private BunBlockNode createForBlockNode(BNode parentNode, String valuesSymbol, String indexSymbol) {
		BunBlockNode forBlockNode = new BunBlockNode(parentNode, null);
		forBlockNode.Append(this.createValueDeclNode(forBlockNode, valuesSymbol, indexSymbol));
		BunBlockNode oldBlockNode = this.blockNode();
		int size = oldBlockNode.GetListSize();
		for(int i = 0; i < size; i++) {
			forBlockNode.Append(oldBlockNode.GetListAt(i));
		}
		return forBlockNode;
	}

	private final String getName() {	//FIXME getName()
		BNode valueNameNode = this.AST[_Value];
		if(!(valueNameNode instanceof GetNameNode)) {
			Utils.fatal(1, "require GetNameNode");
		}
		return ((GetNameNode)valueNameNode).GivenName;
	}

	private final BunBlockNode blockNode() {
		BNode blockNode = this.AST[_Block];
		if(!(blockNode instanceof BunBlockNode)) {
			Utils.fatal(1, "require BlockNode");
		}
		return ((BunBlockNode)blockNode);
	}
}
