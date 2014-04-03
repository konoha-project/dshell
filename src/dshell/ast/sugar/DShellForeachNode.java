package dshell.ast.sugar;

import dshell.ast.DShellForNode;
import dshell.lib.Utils;
import libbun.parser.ast.ZBinaryNode;
import libbun.parser.ast.ZBlockNode;
import libbun.parser.ast.ZBooleanNode;
import libbun.parser.ast.ZComparatorNode;
import libbun.parser.ast.ZDesugarNode;
import libbun.parser.ast.ZErrorNode;
import libbun.parser.ast.ZGetIndexNode;
import libbun.parser.ast.ZGetNameNode;
import libbun.parser.ast.ZIfNode;
import libbun.parser.ast.ZIntNode;
import libbun.parser.ast.ZLetVarNode;
import libbun.parser.ast.ZMethodCallNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZSetNameNode;
import libbun.parser.ast.ZSugarNode;
import libbun.parser.ast.ZVarBlockNode;
import libbun.parser.ZGenerator;
import libbun.parser.ZSource;
import libbun.parser.ZToken;
import libbun.parser.ZTypeChecker;
import libbun.type.ZType;

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
public class DShellForeachNode extends ZSugarNode {
	public final static int _Value = 0;
	public final static int _Expr  = 1;
	public final static int _Block = 2;

	public DShellForeachNode(ZNode ParentNode) {
		super(ParentNode, null, 3);
	}

	@Override
	public ZDesugarNode DeSugar(ZGenerator Generator, ZTypeChecker TypeChecker) {
		TypeChecker.CheckTypeAt(this, _Expr, ZType.VarType);
		if(!this.AST[_Expr].Type.IsArrayType()) {
			return new ZDesugarNode(this, new ZErrorNode(this.ParentNode, this.SourceToken, "require array type")) ;
		}
		String ValuesSymbol = Generator.NameUniqueSymbol("values");
		String SizeSymbol = Generator.NameUniqueSymbol("size");
		String IndexSymbol = Generator.NameUniqueSymbol("index");
		// create if
		ZNode Node = new ZIfNode(this.ParentNode);
		Node.SetNode(ZIfNode._Cond, new ZBooleanNode(true));
		ZBlockNode ThenBlockNode = new ZBlockNode(Node, null);
		Node.SetNode(ZIfNode._Then, ThenBlockNode);
		// create var
		ZVarBlockNode ValuesDeclNode = TypeChecker.CreateVarNode(ThenBlockNode, ValuesSymbol, ZType.VarType, this.AST[_Expr]);
		ThenBlockNode.SetNode(ZNode._AppendIndex, ValuesDeclNode);
		ZVarBlockNode SizeDeclNode = this.CreateSizeDeclNode(ValuesDeclNode, SizeSymbol, ValuesSymbol, TypeChecker);
		ValuesDeclNode.SetNode(ZNode._AppendIndex, SizeDeclNode);
		// create for
		DShellForNode ForNode = new DShellForNode(SizeDeclNode);
		SizeDeclNode.SetNode(ZNode._AppendIndex, ForNode);
		ForNode.SetNode(DShellForNode._Init, TypeChecker.CreateVarNode(ForNode, IndexSymbol, ZType.IntType, new ZIntNode(ForNode, null, 0)));
		ForNode.SetNode(DShellForNode._Cond, this.CreateCondNode(ForNode, IndexSymbol, SizeSymbol));
		ForNode.SetNode(DShellForNode._Next, this.CreateIncrementNode(ForNode, IndexSymbol));
		ForNode.SetNode(DShellForNode._Block, this.CreateForBlockNode(ForNode, ValuesSymbol, IndexSymbol, TypeChecker));
		return new ZDesugarNode(this, Node);
	}

	private ZVarBlockNode CreateSizeDeclNode(ZNode ParentNode, String SizeSymbol, String ValuesSymbol, ZTypeChecker TypeChekcer) {
		ZVarBlockNode Node = TypeChekcer.CreateVarNode(ParentNode, SizeSymbol, ZType.IntType, new ZIntNode(ParentNode, null, 0));
		ZMethodCallNode SizeNode = new ZMethodCallNode(Node, new ZGetNameNode(ParentNode, null, ValuesSymbol));
		SizeNode.SourceToken = this.SourceToken; // for line number
		SizeNode.SetNode(ZMethodCallNode._NameInfo, new ZGetNameNode(SizeNode, null, "Size"));
		SizeNode.GivenName = "Size";
		Node.VarDeclNode().SetNode(ZLetVarNode._InitValue, SizeNode);
		return Node;
	}

	private ZComparatorNode CreateCondNode(ZNode ParentNode, String IndexSymbol, String SizeSymbol) {
		ZNode LeftNode = new ZGetNameNode(ParentNode, null, IndexSymbol);
		ZNode RightNode = new ZGetNameNode(ParentNode, null, SizeSymbol);
		String Operator = "<";
		ZSource Source = new ZSource(this.SourceToken.GetFileName(), this.SourceToken.GetLineNumber(), Operator, this.SourceToken.Source.TokenContext);
		ZToken Token = new ZToken(Source, 0, Operator.length());
		ZComparatorNode Node = new ZComparatorNode(ParentNode, Token, LeftNode, null);
		Node.SetNode(ZBinaryNode._Right, RightNode);
		return Node;
	}

	private ZSetNameNode CreateIncrementNode(ZNode ParentNode, String IndexSymbol) {
		ZSource AddOpSource = new ZSource(this.SourceToken.GetFileName(), this.SourceToken.GetLineNumber(), "+", this.SourceToken.Source.TokenContext);
		ZToken OpToken = new ZToken(AddOpSource, 0, "+".length());
		ZNode BinaryNode = new ZBinaryNode(ParentNode, OpToken, new ZGetNameNode(null, null, IndexSymbol), null);
		BinaryNode.SetNode(ZBinaryNode._Right, new ZIntNode(BinaryNode, null, 1));
		return new ZSetNameNode(IndexSymbol, BinaryNode);
	}

	private ZVarBlockNode CreateValueDeclNode(ZNode ParentNode, String ValuesSymbol, String IndexSymbol, ZTypeChecker TypeChekcer) {
		ZNode GetIndexNode = new ZGetIndexNode(ParentNode, new ZGetNameNode(ParentNode, null, ValuesSymbol));
		GetIndexNode.SetNode(ZGetIndexNode._Index, new ZGetNameNode(GetIndexNode, null, IndexSymbol));
		return TypeChekcer.CreateVarNode(ParentNode, this.GetName(), ZType.VarType, GetIndexNode);
	}

	private ZBlockNode CreateForBlockNode(ZNode ParentNode, String ValuesSymbol, String IndexSymbol, ZTypeChecker TypeChekcer) {
		ZVarBlockNode ForBlockNode = this.CreateValueDeclNode(ParentNode, ValuesSymbol, IndexSymbol, TypeChekcer);
		ZBlockNode OldBlockNode = this.BlockNode();
		int size = OldBlockNode.GetListSize();
		for(int i = 0; i < size; i++) {
			ForBlockNode.Append(OldBlockNode.GetListAt(i));
		}
		return ForBlockNode;
	}

	private final String GetName() {
		ZNode ValueNameNode = this.AST[_Value];
		if(!(ValueNameNode instanceof ZGetNameNode)) {
			Utils.fatal(1, "require GetNameNode");
		}
		return ((ZGetNameNode)ValueNameNode).GetName();
	}

	private final ZBlockNode BlockNode() {
		ZNode BlockNode = this.AST[_Block];
		if(!(BlockNode instanceof ZBlockNode)) {
			Utils.fatal(1, "require BlockNode");
		}
		return ((ZBlockNode)BlockNode);
	}
}