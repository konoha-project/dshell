package dshell.ast.sugar;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BNode;
import libbun.parser.BToken;
import libbun.parser.LibBunVisitor;
import libbun.type.BType;
import libbun.util.BArray;

public class CommandNode extends BNode {
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
	public void Accept(LibBunVisitor Visitor) {
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitCommandNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}