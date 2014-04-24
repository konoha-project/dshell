package dshell.ast.sugar;

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
import libbun.parser.LibBunTypeChecker;
import libbun.type.BType;
import dshell.ast.DShellForNode;
import dshell.lib.Utils;

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

	public DShellForeachNode(BNode ParentNode) {
		super(ParentNode, 3);
	}

	@Override
	public void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType) {
		TypeChecker.CheckTypeAt(this, _Expr, BType.VarType);
		if(!this.AST[_Expr].Type.IsArrayType()) {
			this.SetNode(_Expr, new ErrorNode(this.ParentNode, this.SourceToken, "require array type"));
		}
	}

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker TypeChekcer) {
		String ValuesSymbol = TypeChekcer.Generator.NameUniqueSymbol("values");
		String SizeSymbol = TypeChekcer.Generator.NameUniqueSymbol("size");
		String IndexSymbol = TypeChekcer.Generator.NameUniqueSymbol("index");
		// create if
		BNode Node = new BunIfNode(this.ParentNode);
		Node.SetNode(BunIfNode._Cond, new BunBooleanNode(true));
		BunBlockNode ThenBlockNode = new BunBlockNode(Node, null);
		Node.SetNode(BunIfNode._Then, ThenBlockNode);
		// create var
		this.CreateAndSetValueDeclNode(ThenBlockNode, ValuesSymbol, this.AST[_Expr]);
		this.CreateAndSetSizeDeclNode(ThenBlockNode, SizeSymbol, ValuesSymbol);
		// create for
		DShellForNode ForNode = new DShellForNode(ThenBlockNode);
		ThenBlockNode.SetNode(BNode._AppendIndex, ForNode);
		ForNode.SetNode(DShellForNode._Init, this.CreateIndexDeclNode(ForNode, IndexSymbol));
		ForNode.SetNode(DShellForNode._Cond, this.CreateCondNode(ForNode, IndexSymbol, SizeSymbol));
		ForNode.SetNode(DShellForNode._Next, this.CreateIncrementNode(ForNode, IndexSymbol));
		ForNode.SetNode(DShellForNode._Block, this.CreateForBlockNode(ForNode, ValuesSymbol, IndexSymbol));
		return new DesugarNode(this, Node);
	}

	private void CreateAndSetValueDeclNode(BunBlockNode ParentNode, String ValuesSymbol, BNode ExprNode) {
		BunLetVarNode Node = new BunLetVarNode(ParentNode, BunLetVarNode._IsReadOnly, null, ValuesSymbol);
		Node.SetNode(BunLetVarNode._InitValue, ExprNode);
		ParentNode.SetNode(BNode._AppendIndex, Node);
	}

	private void CreateAndSetSizeDeclNode(BunBlockNode ParentNode, String SizeSymbol, String ValuesSymbol) {
		BunLetVarNode Node = new BunLetVarNode(ParentNode, BunLetVarNode._IsReadOnly, null, SizeSymbol);
		Node.SourceToken = this.SourceToken;
		MethodCallNode SizeNode = new MethodCallNode(ParentNode, new GetNameNode(ParentNode, null, ValuesSymbol), "Size");
		SizeNode.SourceToken = this.SourceToken; // for line number
		Node.SetNode(BunLetVarNode._InitValue, SizeNode);
		ParentNode.SetNode(BNode._AppendIndex, Node);
	}

	private BunLetVarNode CreateIndexDeclNode(BNode ParentNode, String IndexSymbol) {
		BunLetVarNode Node = new BunLetVarNode(ParentNode, 0, null, IndexSymbol);
		Node.SetNode(BunLetVarNode._InitValue, new BunIntNode(Node, null, 0));
		return Node;
	}

	private ComparatorNode CreateCondNode(BNode ParentNode, String IndexSymbol, String SizeSymbol) {
		BNode LeftNode = new GetNameNode(ParentNode, null, IndexSymbol);
		BNode RightNode = new GetNameNode(ParentNode, null, SizeSymbol);
		BunLessThanNode Node = new BunLessThanNode(ParentNode);
		Node.SetLeftNode(LeftNode);
		Node.SetRightNode(RightNode);
		return Node;
	}

	private AssignNode CreateIncrementNode(BNode ParentNode, String IndexSymbol) {
		BinaryOperatorNode BinaryNode = new BunAddNode(ParentNode);
		BinaryNode.SetLeftNode(new GetNameNode(BinaryNode, null, IndexSymbol));
		BinaryNode.SetRightNode(new BunIntNode(BinaryNode, null, 1));
		return new AssignNode(IndexSymbol, BinaryNode);
	}

	private BunLetVarNode CreateValueDeclNode(BNode ParentNode, String ValuesSymbol, String IndexSymbol) {
		BNode IndexNode = new GetIndexNode(ParentNode, new GetNameNode(ParentNode, null, ValuesSymbol));
		IndexNode.SetNode(GetIndexNode._Index, new GetNameNode(IndexNode, null, IndexSymbol));
		BunLetVarNode Node = new BunLetVarNode(ParentNode, 0, null, this.GetName());
		Node.SetNode(BunLetVarNode._InitValue, IndexNode);
		return Node;
	}

	private BunBlockNode CreateForBlockNode(BNode ParentNode, String ValuesSymbol, String IndexSymbol) {
		BunBlockNode ForBlockNode = new BunBlockNode(ParentNode, null);
		ForBlockNode.Append(this.CreateValueDeclNode(ForBlockNode, ValuesSymbol, IndexSymbol));
		BunBlockNode OldBlockNode = this.BlockNode();
		int size = OldBlockNode.GetListSize();
		for(int i = 0; i < size; i++) {
			ForBlockNode.Append(OldBlockNode.GetListAt(i));
		}
		return ForBlockNode;
	}

	private final String GetName() {	//FIXME getName()
		BNode ValueNameNode = this.AST[_Value];
		if(!(ValueNameNode instanceof GetNameNode)) {
			Utils.fatal(1, "require GetNameNode");
		}
		return ((GetNameNode)ValueNameNode).GivenName;
	}

	private final BunBlockNode BlockNode() {
		BNode BlockNode = this.AST[_Block];
		if(!(BlockNode instanceof BunBlockNode)) {
			Utils.fatal(1, "require BlockNode");
		}
		return ((BunBlockNode)BlockNode);
	}
}
