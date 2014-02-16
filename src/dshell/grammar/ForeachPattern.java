package dshell.grammar;

import java.util.Random;

import zen.ast.ZBinaryNode;
import zen.ast.ZBlockNode;
import zen.ast.ZComparatorNode;
import zen.ast.ZGetIndexNode;
import zen.ast.ZGetNameNode;
import zen.ast.ZIntNode;
import zen.ast.ZMethodCallNode;
import zen.ast.ZNode;
import zen.ast.ZSetNameNode;
import zen.ast.ZVarNode;
import zen.ast.ZWhileNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZSource;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

/*
 * for(value in $Expression$) {
 *     $Block$
 * }
 * ==>
 * var index = 0
 * var valueList = $Expression$
 * var size = valueList.Size()
 * while(index < size) {
 *     var value = valueList[index]
 *     $Block$
 *     index = index + 1
 * }
 * */
public class ForeachPattern extends ZMatchFunction {
	private final Random rand;
	private String FileName;
	private int LineNum;
	private String IndexName;
	private String ValueListName;
	private String SizeName;
	private String ValueName;

	public ForeachPattern() {
		this.rand = new Random();
	}
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {	
		this.IndexName = "___" + this.rand.nextInt(1000) + "_foreach_index_" + this.rand.nextInt(1000);
		this.ValueListName = "___" + this.rand.nextInt(1000) + "_foreach_valueList_" + this.rand.nextInt(1000);
		this.SizeName = "___" + this.rand.nextInt(1000) + "_foreach_size_" + this.rand.nextInt(1000);
		ZToken ContextToken = TokenContext.GetToken();
		this.FileName = ContextToken.GetFileName();
		this.LineNum = ContextToken.GetLineNumber();

		// var index = 0
		ZNode Node = this.CreateIndexDeclNode(ParentNode, TokenContext);

		// var valueList = $Expression$
		ZNode ValueListDeclNode = this.MatchAndCreateValueListDeclNode(Node, TokenContext);
		if(ValueListDeclNode.IsErrorNode()) {
			return ValueListDeclNode;
		}

		// var size = valList.Size()
		ZNode SizeDeclNode = this.CreateSizeDeclNode(ValueListDeclNode, TokenContext);

		// var index = 0 { var valueList = $Expression$ }
		Node.Set(ZNode._AppendIndex, ValueListDeclNode);

		// var valueList = $Expression$ { var size = valueList.Size() }
		ValueListDeclNode.Set(ZNode._AppendIndex, SizeDeclNode);

		// while(index < size)
		ZNode WhileNode = new ZWhileNode(SizeDeclNode);
		WhileNode.Set(ZWhileNode._Cond, this.CreateCondNode(WhileNode, TokenContext));

		// var size = valueList.Size() { while(index < size) }
		SizeDeclNode.Set(ZNode._AppendIndex, WhileNode);

		// { $WhileBlock$}
		ZNode BlockNode = this.MatchAndCreateBlockNode(WhileNode, TokenContext);
		if(BlockNode.IsErrorNode()) {
			return BlockNode;
		}

		// while(index < size) { $WhileBlock$ }
		WhileNode.Set(ZWhileNode._Block, BlockNode);
		return Node;
	}

	private ZVarNode CreateIndexDeclNode(ZNode ParentNode, ZTokenContext TokenContext) {
		ZVarNode Node = this.CreateVarNode(ParentNode, TokenContext, this.IndexName);
		ZSource Source = new ZSource(this.FileName, this.LineNum, "0", TokenContext);
		ZToken Token = new ZToken(Source, 0, "0".length());
		Node.Set(ZVarNode._InitValue, new ZIntNode(Node, Token, 0));
		return Node;
	}

	private ZNode MatchAndCreateValueListDeclNode(ZNode ParentNode, ZTokenContext TokenContext) {
		// var valueList
		ZNode Node = this.CreateVarNode(ParentNode, TokenContext, this.ValueListName);

		ZNode DummyNode = new ZWhileNode(ParentNode);	// dummy
		DummyNode = TokenContext.MatchToken(DummyNode, "for", ZTokenContext.Required);
		DummyNode = TokenContext.MatchToken(DummyNode, "(", ZTokenContext.Required);
		if(DummyNode.IsErrorNode()) {
			return DummyNode;
		}
		ZNode ValueNode = TokenContext.ParsePattern(ParentNode, "$Name$", ZTokenContext.Required);
		if(ValueNode.IsErrorNode()) {
			return ValueNode;
		}
		this.ValueName = ((ZGetNameNode)ValueNode).VarName;	// set value name
		DummyNode = TokenContext.MatchToken(DummyNode, "in", ZTokenContext.Required);
		if(DummyNode.IsErrorNode()) {
			return DummyNode;
		}

		// $Expression$
		ZNode ExprNode = TokenContext.ParsePattern(Node, "$Expression$", ZTokenContext.Required);
		if(ExprNode.IsErrorNode()) {
			return ExprNode;
		}
		DummyNode = TokenContext.MatchToken(DummyNode, ")", ZTokenContext.Required);
		if(DummyNode.IsErrorNode()) {
			return DummyNode;
		}

		// var valueList = $Expression$
		Node.Set(ZVarNode._InitValue, ExprNode);
		return Node;
	}

	private ZVarNode CreateSizeDeclNode(ZNode ParentNode, ZTokenContext TokenContext) {
		// var size
		ZVarNode Node = this.CreateVarNode(ParentNode, TokenContext, this.SizeName);

		// valueList.Size()
		ZMethodCallNode SizeNode = new ZMethodCallNode(Node, this.CreateNameNode(ParentNode, TokenContext, this.ValueListName));
		SizeNode.Set(ZNode._NameInfo, this.CreateNameNode(SizeNode, TokenContext, "Size"));

		// var size = valueList.Size()
		Node.Set(ZVarNode._InitValue, SizeNode);
		return Node;
	}

	private ZComparatorNode CreateCondNode(ZNode ParentNode, ZTokenContext TokenContext) {
		ZNode LeftNode = this.CreateNameNode(ParentNode, TokenContext, this.IndexName);
		ZNode RightNode = this.CreateNameNode(ParentNode, TokenContext, this.SizeName);
		String Operator = "<";
		ZSource Source = new ZSource(LeftNode.SourceToken.GetFileName(), LeftNode.SourceToken.GetLineNumber(), Operator, TokenContext);
		ZToken Token = new ZToken(Source, 0, Operator.length());
		ZComparatorNode Node = new ZComparatorNode(ParentNode, Token, LeftNode, null);
		Node.Set(ZBinaryNode._Right, RightNode);
		return Node;
	}

	/*
	 * {
	 *     var value = valueList[index]
	 *     $Block$
	 *     index = index + 1
	 * }
	 * */
	private ZNode MatchAndCreateBlockNode(ZNode ParentNode, ZTokenContext TokenContext) {
		ZNode Node = new ZBlockNode(ParentNode, 0);
		// var value
		ZNode ValueDeclNode = this.CreateVarNode(Node, TokenContext, this.ValueName);
		Node.Set(ZNode._AppendIndex, ValueDeclNode);

		// valueList[index]
		ZNode GetIndexNode = new ZGetIndexNode(ValueDeclNode, this.CreateNameNode(ValueDeclNode, TokenContext, this.ValueListName));
		GetIndexNode.Set(ZGetIndexNode._Index, this.CreateNameNode(GetIndexNode, TokenContext, this.IndexName));

		// var value = valueList[index]
		ValueDeclNode.Set(ZVarNode._InitValue, GetIndexNode);

		// $Block$
		ZNode BlockNode = TokenContext.ParsePattern(ValueDeclNode, "$Block$", ZTokenContext.Required);
		if(BlockNode.IsErrorNode()) {
			return BlockNode;
		}

		// var value = valueList[index] { $Block$ }
		ValueDeclNode.Set(ZNode._AppendIndex, BlockNode);

		// $Block$ ... { index = index + 1 }
		ZBlockNode BottomBlockNode = this.FindBottomBlockNode((ZBlockNode) BlockNode);
		ZNode IncNode = this.CreateIncrementNode(BottomBlockNode, TokenContext);
		BottomBlockNode.Append(IncNode);
		return Node;
	}

	private ZBlockNode FindBottomBlockNode(ZBlockNode BlockNode) {
		ZNode LastNode = BlockNode.GetListAt(BlockNode.GetListSize() - 1);
		if(LastNode instanceof ZBlockNode) {
			return this.FindBottomBlockNode((ZBlockNode) LastNode);
		}
		return BlockNode;
	}

	private ZNode CreateIncrementNode(ZNode ParentNode, ZTokenContext TokenContext) {
		// index 
		ZSource NameZource = new ZSource(this.FileName, this.LineNum, this.IndexName, TokenContext);
		ZToken Token = new ZToken(NameZource, 0, this.IndexName.length());
		ZNode Node = new ZSetNameNode(ParentNode, Token, this.IndexName);

		// index + 1 		FIXME: BinaryNode Parent
		ZSource AddOpSource = new ZSource(this.FileName, this.LineNum, "+", TokenContext);
		ZToken OpToken = new ZToken(AddOpSource, 0, "+".length());
		ZNode BinaryNode = new ZBinaryNode(Node, OpToken, this.CreateNameNode(Node, TokenContext, this.IndexName), null);
		BinaryNode.Set(ZBinaryNode._Right, new ZIntNode(Node, null, 1));

		// index = index + 1
		Node.Set(ZSetNameNode._Expr, BinaryNode);
		return Node;
	}

	private ZVarNode CreateVarNode(ZNode ParentNode, ZTokenContext TokenContext, String VarName) {
		ZVarNode Node = new ZVarNode(ParentNode);
		Node.Set(ZNode._NameInfo, this.CreateNameNode(Node, TokenContext, VarName));
		return Node;
	}

	private ZGetNameNode CreateNameNode(ZNode ParentNode, ZTokenContext TokenContext, String Name) {
		ZSource NameZource = new ZSource(this.FileName, this.LineNum, Name, TokenContext);
		ZToken Token = new ZToken(NameZource, 0, Name.length());
		return new ZGetNameNode(ParentNode, Token, Name);
	}
}
