package dshell.ast.sugar;

import libbun.ast.BBlockNode;
import libbun.ast.BDesugarNode;
import libbun.ast.BNode;
import libbun.ast.BSugarNode;
import libbun.ast.binary.BBinaryNode;
import libbun.ast.binary.ZComparatorNode;
import libbun.ast.decl.BLetVarNode;
import libbun.ast.decl.ZVarBlockNode;
import libbun.ast.error.BErrorNode;
import libbun.ast.expression.BGetIndexNode;
import libbun.ast.expression.BGetNameNode;
import libbun.ast.expression.BMethodCallNode;
import libbun.ast.expression.BSetNameNode;
import libbun.ast.literal.BBooleanNode;
import libbun.ast.literal.BIntNode;
import libbun.ast.statement.BIfNode;
import libbun.parser.BGenerator;
import libbun.parser.BSource;
import libbun.parser.BToken;
import libbun.parser.BTypeChecker;
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
public class DShellForeachNode extends BSugarNode {
	public final static int _Value = 0;
	public final static int _Expr  = 1;
	public final static int _Block = 2;

	public DShellForeachNode(BNode ParentNode) {
		super(ParentNode, null, 3);
	}

	@Override
	public BDesugarNode DeSugar(BGenerator Generator, BTypeChecker TypeChecker) {
		TypeChecker.CheckTypeAt(this, _Expr, BType.VarType);
		if(!this.AST[_Expr].Type.IsArrayType()) {
			return new BDesugarNode(this, new BErrorNode(this.ParentNode, this.SourceToken, "require array type")) ;
		}
		String ValuesSymbol = Generator.NameUniqueSymbol("values");
		String SizeSymbol = Generator.NameUniqueSymbol("size");
		String IndexSymbol = Generator.NameUniqueSymbol("index");
		// create if
		BNode Node = new BIfNode(this.ParentNode);
		Node.SetNode(BIfNode._Cond, new BBooleanNode(true));
		BBlockNode ThenBlockNode = new BBlockNode(Node, null);
		Node.SetNode(BIfNode._Then, ThenBlockNode);
		// create var
		ZVarBlockNode ValuesDeclNode = TypeChecker.CreateVarNode(ThenBlockNode, ValuesSymbol, BType.VarType, this.AST[_Expr]);
		ThenBlockNode.SetNode(BNode._AppendIndex, ValuesDeclNode);
		ZVarBlockNode SizeDeclNode = this.CreateSizeDeclNode(ValuesDeclNode, SizeSymbol, ValuesSymbol, TypeChecker);
		ValuesDeclNode.SetNode(BNode._AppendIndex, SizeDeclNode);
		// create for
		DShellForNode ForNode = new DShellForNode(SizeDeclNode);
		SizeDeclNode.SetNode(BNode._AppendIndex, ForNode);
		ForNode.SetNode(DShellForNode._Init, TypeChecker.CreateVarNode(ForNode, IndexSymbol, BType.IntType, new BIntNode(ForNode, null, 0)));
		ForNode.SetNode(DShellForNode._Cond, this.CreateCondNode(ForNode, IndexSymbol, SizeSymbol));
		ForNode.SetNode(DShellForNode._Next, this.CreateIncrementNode(ForNode, IndexSymbol));
		ForNode.SetNode(DShellForNode._Block, this.CreateForBlockNode(ForNode, ValuesSymbol, IndexSymbol, TypeChecker));
		return new BDesugarNode(this, Node);
	}

	private ZVarBlockNode CreateSizeDeclNode(BNode ParentNode, String SizeSymbol, String ValuesSymbol, BTypeChecker TypeChekcer) {
		ZVarBlockNode Node = TypeChekcer.CreateVarNode(ParentNode, SizeSymbol, BType.IntType, new BIntNode(ParentNode, null, 0));
		BMethodCallNode SizeNode = new BMethodCallNode(Node, new BGetNameNode(ParentNode, null, ValuesSymbol));
		SizeNode.SourceToken = this.SourceToken; // for line number
		SizeNode.SetNode(BMethodCallNode._NameInfo, new BGetNameNode(SizeNode, null, "Size"));
		SizeNode.GivenName = "Size";
		Node.VarDeclNode().SetNode(BLetVarNode._InitValue, SizeNode);
		return Node;
	}

	private ZComparatorNode CreateCondNode(BNode ParentNode, String IndexSymbol, String SizeSymbol) {
		BNode LeftNode = new BGetNameNode(ParentNode, null, IndexSymbol);
		BNode RightNode = new BGetNameNode(ParentNode, null, SizeSymbol);
		String Operator = "<";
		BSource Source = new BSource(this.SourceToken.GetFileName(), this.SourceToken.GetLineNumber(), Operator, this.SourceToken.Source.TokenContext);
		BToken Token = new BToken(Source, 0, Operator.length());
		ZComparatorNode Node = new ZComparatorNode(ParentNode, Token, LeftNode, null);
		Node.SetNode(BBinaryNode._Right, RightNode);
		return Node;
	}

	private BSetNameNode CreateIncrementNode(BNode ParentNode, String IndexSymbol) {
		BSource AddOpSource = new BSource(this.SourceToken.GetFileName(), this.SourceToken.GetLineNumber(), "+", this.SourceToken.Source.TokenContext);
		BToken OpToken = new BToken(AddOpSource, 0, "+".length());
		BNode BinaryNode = new BBinaryNode(ParentNode, OpToken, new BGetNameNode(null, null, IndexSymbol), null);
		BinaryNode.SetNode(BBinaryNode._Right, new BIntNode(BinaryNode, null, 1));
		return new BSetNameNode(IndexSymbol, BinaryNode);
	}

	private ZVarBlockNode CreateValueDeclNode(BNode ParentNode, String ValuesSymbol, String IndexSymbol, BTypeChecker TypeChekcer) {
		BNode GetIndexNode = new BGetIndexNode(ParentNode, new BGetNameNode(ParentNode, null, ValuesSymbol));
		GetIndexNode.SetNode(BGetIndexNode._Index, new BGetNameNode(GetIndexNode, null, IndexSymbol));
		return TypeChekcer.CreateVarNode(ParentNode, this.GetName(), BType.VarType, GetIndexNode);
	}

	private BBlockNode CreateForBlockNode(BNode ParentNode, String ValuesSymbol, String IndexSymbol, BTypeChecker TypeChekcer) {
		ZVarBlockNode ForBlockNode = this.CreateValueDeclNode(ParentNode, ValuesSymbol, IndexSymbol, TypeChekcer);
		BBlockNode OldBlockNode = this.BlockNode();
		int size = OldBlockNode.GetListSize();
		for(int i = 0; i < size; i++) {
			ForBlockNode.Append(OldBlockNode.GetListAt(i));
		}
		return ForBlockNode;
	}

	private final String GetName() {	//FIXME getName()
		BNode ValueNameNode = this.AST[_Value];
		if(!(ValueNameNode instanceof BGetNameNode)) {
			Utils.fatal(1, "require GetNameNode");
		}
		return ((BGetNameNode)ValueNameNode).GivenName;
	}

	private final BBlockNode BlockNode() {
		BNode BlockNode = this.AST[_Block];
		if(!(BlockNode instanceof BBlockNode)) {
			Utils.fatal(1, "require BlockNode");
		}
		return ((BBlockNode)BlockNode);
	}
}
