package dshell.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.literal.BunArrayLiteralNode;
import libbun.parser.BToken;
import libbun.parser.LibBunTypeChecker;
import libbun.type.BType;
import libbun.util.BArray;

public class CommandNode extends SyntaxSugarNode {
	private final BArray<BNode> ArgList;
	private BType RetType = BType.VarType;
	public CommandNode PipedNextNode;

	public CommandNode(BNode ParentNode, BToken Token, String Command) {
		super(ParentNode, 0);
		this.SourceToken = Token;
		this.PipedNextNode = null;
		this.ArgList = new BArray<BNode>(new BNode[]{});
		this.AppendArgNode(new ArgumentNode(ParentNode, Command));
	}

	public void AppendArgNode(BNode Node) {
		this.ArgList.add(this.SetChild(Node, true));
	}

	public BNode AppendPipedNextNode(CommandNode Node) {
		CommandNode CurrentNode = this;
		while(CurrentNode.PipedNextNode != null) {
			CurrentNode = CurrentNode.PipedNextNode;
		}
		CurrentNode.PipedNextNode = (CommandNode) CurrentNode.SetChild(Node, false);
		return this;
	}

	public int GetArgSize() {
		return this.ArgList.size();
	}

	public void SetArgAt(int Index, BNode ArgNode) {
		BArray.SetIndex(this.ArgList, Index, ArgNode);
	}

	public BNode GetArgAt(int Index) {
		return BArray.GetIndex(this.ArgList, Index);
	}

	public void SetType(BType Type) {
		this.RetType = Type;
	}

	public BType RetType() {
		return this.RetType;
	}

	@Override
	public void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType) {
		if(this.RetType().IsVarType()) {
			if(ContextType.IsBooleanType() || ContextType.IsIntType() || ContextType.IsStringType()) {
				this.SetType(ContextType);
			}
			else if(ContextType.IsVarType() && !(this.ParentNode instanceof BunBlockNode)) {
				this.SetType(BType.StringType);
			}
			else {
				this.SetType(BType.IntType);
			}
		}
	}

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker TypeChecker) {
		String FuncName = "ExecCommandInt";
		if(this.RetType().IsBooleanType()) {
			FuncName = "ExecCommandBoolean";
		}
		else if(this.RetType().IsStringType()) {
			FuncName = "ExecCommandString";
		}
		BunArrayLiteralNode ArrayNode = new BunArrayLiteralNode(this.ParentNode);
		CommandNode CurrentNode = this;
		while(CurrentNode != null) {
			BunArrayLiteralNode SubArrayNode = new BunArrayLiteralNode(ArrayNode);
			int size = CurrentNode.GetArgSize();
			int i = 0;
			while(i < size) {
				SubArrayNode.Append(CurrentNode.GetArgAt(i));
				i = i + 1;
			}
			ArrayNode.Append(SubArrayNode);
			CurrentNode = CurrentNode.PipedNextNode;
		}
		FuncCallNode Node = new FuncCallNode(this.ParentNode, new GetNameNode(this.ParentNode, null, FuncName));
		Node.Append(ArrayNode);
		return new DesugarNode(this, Node);
	}
}