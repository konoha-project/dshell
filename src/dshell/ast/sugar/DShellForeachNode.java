package dshell.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.binary.BunLessThanNode;
import libbun.ast.binary.ComparatorNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.expression.GetIndexNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.expression.MethodCallNode;
import libbun.ast.expression.SetNameNode;
import libbun.ast.literal.BunBooleanNode;
import libbun.ast.literal.BunIntNode;
import libbun.ast.statement.BunIfNode;
import libbun.encode.AbstractGenerator;
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
public class DShellForeachNode extends SyntaxSugarNode {
	public final static int _Value = 0;
	public final static int _Expr  = 1;
	public final static int _Block = 2;

	public DShellForeachNode(BNode ParentNode) {
		super(ParentNode, null, 3);
	}

	@Override
	public DesugarNode DeSugar(AbstractGenerator Generator, BTypeChecker TypeChecker) {
		TypeChecker.CheckTypeAt(this, _Expr, BType.VarType);
		if(!this.AST[_Expr].Type.IsArrayType()) {
			return new DesugarNode(this, new ErrorNode(this.ParentNode, this.SourceToken, "require array type"));
		}
		String ValuesSymbol = Generator.NameUniqueSymbol("values");
		String SizeSymbol = Generator.NameUniqueSymbol("size");
		String IndexSymbol = Generator.NameUniqueSymbol("index");
		// create if
		BNode Node = new BunIfNode(this.ParentNode);
		Node.SetNode(BunIfNode._Cond, new BunBooleanNode(true));
		BunBlockNode ThenBlockNode = new BunBlockNode(Node, null);
		Node.SetNode(BunIfNode._Then, ThenBlockNode);
		// create var
		BunVarBlockNode ValuesDeclNode = TypeChecker.CreateVarNode(ThenBlockNode, ValuesSymbol, BType.VarType, this.AST[_Expr]);
		ThenBlockNode.SetNode(BNode._AppendIndex, ValuesDeclNode);
		BunVarBlockNode SizeDeclNode = this.CreateSizeDeclNode(ValuesDeclNode, SizeSymbol, ValuesSymbol, TypeChecker);
		ValuesDeclNode.SetNode(BNode._AppendIndex, SizeDeclNode);
		// create for
		DShellForNode ForNode = new DShellForNode(SizeDeclNode);
		SizeDeclNode.SetNode(BNode._AppendIndex, ForNode);
		ForNode.SetNode(DShellForNode._Init, TypeChecker.CreateVarNode(ForNode, IndexSymbol, BType.IntType, new BunIntNode(ForNode, null, 0)));
		ForNode.SetNode(DShellForNode._Cond, this.CreateCondNode(ForNode, IndexSymbol, SizeSymbol));
		ForNode.SetNode(DShellForNode._Next, this.CreateIncrementNode(ForNode, IndexSymbol));
		ForNode.SetNode(DShellForNode._Block, this.CreateForBlockNode(ForNode, ValuesSymbol, IndexSymbol, TypeChecker));
		return new DesugarNode(this, Node);
	}

	private BunVarBlockNode CreateSizeDeclNode(BNode ParentNode, String SizeSymbol, String ValuesSymbol, BTypeChecker TypeChekcer) {
		BunVarBlockNode Node = TypeChekcer.CreateVarNode(ParentNode, SizeSymbol, BType.IntType, new BunIntNode(ParentNode, null, 0));
		MethodCallNode SizeNode = new MethodCallNode(Node, new GetNameNode(ParentNode, null, ValuesSymbol));
		SizeNode.SourceToken = this.SourceToken; // for line number
		SizeNode.SetNode(MethodCallNode._NameInfo, new GetNameNode(SizeNode, null, "Size"));
		SizeNode.GivenName = "Size";
		Node.VarDeclNode().SetNode(BunLetVarNode._InitValue, SizeNode);
		return Node;
	}

	private ComparatorNode CreateCondNode(BNode ParentNode, String IndexSymbol, String SizeSymbol) {
		BNode LeftNode = new GetNameNode(ParentNode, null, IndexSymbol);
		BNode RightNode = new GetNameNode(ParentNode, null, SizeSymbol);
		String Operator = "<";
		BSource Source = new BSource(this.SourceToken.GetFileName(), this.SourceToken.GetLineNumber(), Operator, this.SourceToken.Source.TokenContext);
		BToken Token = new BToken(Source, 0, Operator.length());
		BunLessThanNode Node = new BunLessThanNode(ParentNode, Token, LeftNode);
		Node.SetNode(BinaryOperatorNode._Right, RightNode);
		return Node;
	}

	private SetNameNode CreateIncrementNode(BNode ParentNode, String IndexSymbol) {
		BSource AddOpSource = new BSource(this.SourceToken.GetFileName(), this.SourceToken.GetLineNumber(), "+", this.SourceToken.Source.TokenContext);
		BToken OpToken = new BToken(AddOpSource, 0, "+".length());
		BNode BinaryNode = new BunAddNode(ParentNode, OpToken, new GetNameNode(null, null, IndexSymbol));
		BinaryNode.SetNode(BinaryOperatorNode._Right, new BunIntNode(BinaryNode, null, 1));
		return new SetNameNode(IndexSymbol, BinaryNode);
	}

	private BunVarBlockNode CreateValueDeclNode(BNode ParentNode, String ValuesSymbol, String IndexSymbol, BTypeChecker TypeChekcer) {
		BNode IndexNode = new GetIndexNode(ParentNode, new GetNameNode(ParentNode, null, ValuesSymbol));
		IndexNode.SetNode(GetIndexNode._Index, new GetNameNode(IndexNode, null, IndexSymbol));
		return TypeChekcer.CreateVarNode(ParentNode, this.GetName(), BType.VarType, IndexNode);
	}

	private BunBlockNode CreateForBlockNode(BNode ParentNode, String ValuesSymbol, String IndexSymbol, BTypeChecker TypeChekcer) {
		BunVarBlockNode ForBlockNode = this.CreateValueDeclNode(ParentNode, ValuesSymbol, IndexSymbol, TypeChekcer);
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
