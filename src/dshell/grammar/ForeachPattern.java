package dshell.grammar;

import java.util.Random;

import zen.ast.ZBinaryNode;
import zen.ast.ZComparatorNode;
import zen.ast.ZGetIndexNode;
import zen.ast.ZGetNameNode;
import zen.ast.ZIntNode;
import zen.ast.ZMethodCallNode;
import zen.ast.ZNode;
import zen.ast.ZVarDeclNode;
import zen.ast.ZWhileNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZSource;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

/*
 * for(val in valList) {
 *     $Block$
 * }
 * 
 * var index = 0
 * var size = valList.Size()
 * while(index < size) {
 *     var val = valList[index]
 *     $Block$
 *     index = index + 1
 * }
 * */
public class ForeachPattern extends ZMatchFunction {
	private final Random rand;
	private String IndexSymbol;
	private String SizeSymbol;
	private ZNode VarNameNode;
	public ForeachPattern() {
		this.rand = new Random();
	}
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {	
		this.IndexSymbol = "___" + this.rand.nextInt(1000) + "_foreach_init_" + this.rand.nextInt(1000);
		this.SizeSymbol = "___" + this.rand.nextInt(1000) + "_foreach_size_" + this.rand.nextInt(1000);
		this.VarNameNode = null;

		ZToken ContextToken = TokenContext.GetToken();
		ZNode Node = this.CreateIndexDeclNode(ParentNode, TokenContext, ContextToken);
		ZNode SizeDeclNode = this.MatchAndCreateSizeDeclNode(Node, TokenContext);
		if(SizeDeclNode.IsErrorNode()) {
			return SizeDeclNode;
		}
		Node.Set(ZNode._AppendIndex, SizeDeclNode);
		ZNode WhileNode = new ZWhileNode(SizeDeclNode);
		SizeDeclNode.Set(ZNode._AppendIndex, WhileNode);
		WhileNode.Set(ZWhileNode._Cond, this.CreateCondNode(WhileNode, TokenContext));
		ZNode BlockNode = this.MatchAndCreateBlockNode(WhileNode, TokenContext);
		if(BlockNode.IsErrorNode()) {
			return BlockNode;
		}
		WhileNode.Set(ZWhileNode._Block, BlockNode);
		return Node;
	}

	private ZVarDeclNode CreateIndexDeclNode(ZNode ParentNode, ZTokenContext TokenContext, ZToken ContextToken) {
		ZVarDeclNode Node = this.CreateVarDeclNode(ParentNode, TokenContext, ContextToken, this.IndexSymbol);
		ZSource Source = new ZSource(ContextToken.GetFileName(), ContextToken.GetLineNumber(), "0", TokenContext);
		ZToken Token = new ZToken(Source, 0, "0".length());
		Node.Set(ZVarDeclNode._InitValue, new ZIntNode(ParentNode, Token, 0));
		return Node;
	}

	private ZNode MatchAndCreateSizeDeclNode(ZNode ParentNode, ZTokenContext TokenContext) {
		ZNode DummyNode = new ZWhileNode(ParentNode);	// dummy
		DummyNode = TokenContext.MatchToken(DummyNode, "for", ZTokenContext.Required);
		DummyNode = TokenContext.MatchToken(DummyNode, "(", ZTokenContext.Required);
		if(DummyNode.IsErrorNode()) {
			return DummyNode;
		}
		this.VarNameNode = TokenContext.ParsePattern(ParentNode, "$Name$", ZTokenContext.Required);
		if(this.VarNameNode.IsErrorNode()) {
			return this.VarNameNode;
		}
		DummyNode = TokenContext.MatchToken(DummyNode, "in", ZTokenContext.Required);
		if(DummyNode.IsErrorNode()) {
			return DummyNode;
		}
		ZNode ExprNode = TokenContext.ParsePattern(ParentNode, "$Expression$", ZTokenContext.Required);
		if(ExprNode.IsErrorNode()) {
			return ExprNode;
		}
		DummyNode = TokenContext.MatchToken(DummyNode, ")", ZTokenContext.Required);
		if(DummyNode.IsErrorNode()) {
			return DummyNode;
		}
		return this.CreateSizeDeclNode(ParentNode, TokenContext, ExprNode);
	}

	private ZComparatorNode CreateCondNode(ZNode ParentNode, ZTokenContext TokenContext) {
		ZNode LeftNode = this.CreateNameNode(ParentNode, TokenContext, ParentNode.SourceToken, this.IndexSymbol);
		ZNode RightNode = this.CreateNameNode(ParentNode, TokenContext, ParentNode.SourceToken, this.SizeSymbol);
		String Operator = "<";
		ZSource Source = new ZSource(LeftNode.SourceToken.GetFileName(), LeftNode.SourceToken.GetLineNumber(), Operator, TokenContext);
		ZToken Token = new ZToken(Source, 0, Operator.length());
		ZComparatorNode Node = new ZComparatorNode(ParentNode, Token, LeftNode, null);
		Node.Set(ZBinaryNode._Right, RightNode);
		return Node;
	}

	private ZNode MatchAndCreateBlockNode(ZNode ParentNode, ZTokenContext TokenContext) {
		ZNode Node = new ZVarDeclNode(ParentNode);
		this.VarNameNode.ParentNode = Node;
		Node.Set(ZVarDeclNode._NameInfo, this.VarNameNode);
		ZNode GetIndexNode = new ZGetIndexNode(Node, this.CreateNameNode(Node, TokenContext, this.VarNameNode.SourceToken, this.IndexSymbol));
		Node.Set(ZVarDeclNode._InitValue, GetIndexNode);
		ZNode BlockNode = TokenContext.ParsePattern(Node, "$Block$", ZTokenContext.Required);
		if(BlockNode.IsErrorNode()) {
			return BlockNode;
		}
		
		return null;
	}

	private ZVarDeclNode CreateSizeDeclNode(ZNode ParentNode, ZTokenContext TokenContext, ZNode ExprNode) {
		ZVarDeclNode Node = this.CreateVarDeclNode(ParentNode, TokenContext, ExprNode.SourceToken, this.SizeSymbol);
		Node.Set(ZVarDeclNode._InitValue, this.CreateSizeNode(ParentNode, TokenContext, ExprNode));
		return Node;
	}

	private ZVarDeclNode CreateVarDeclNode(ZNode ParentNode, ZTokenContext TokenContext, ZToken ContextToken, String VarName) {
		ZVarDeclNode Node = new ZVarDeclNode(ParentNode);
		Node.Set(ZNode._NameInfo, this.CreateNameNode(ParentNode, TokenContext, ContextToken, VarName));
		return Node;
	}

	private ZGetNameNode CreateNameNode(ZNode ParentNode, ZTokenContext TokenContext, ZToken ContextToken, String Name) {
		int LineNum = ContextToken.GetLineNumber();
		String FileName = ContextToken.GetFileName();
		ZSource NameZource = new ZSource(FileName, LineNum, Name, TokenContext);
		ZToken Token = new ZToken(NameZource, 0, Name.length());
		return new ZGetNameNode(ParentNode, Token, Name);
	}

	private ZMethodCallNode CreateSizeNode(ZNode ParentNode, ZTokenContext TokenContext, ZNode RecvNode) {
		ZMethodCallNode Node = new ZMethodCallNode(ParentNode, RecvNode);
		Node.Set(ZNode._NameInfo, this.CreateNameNode(ParentNode, TokenContext, RecvNode.SourceToken, "Size"));
		return Node;
	}
}
