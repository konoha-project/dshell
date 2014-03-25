package dshell.ast.sugar;

import java.util.ArrayList;


import zen.ast.ZDesugarNode;
import zen.ast.ZNode;
import zen.ast.ZSugarNode;
import zen.parser.ZGenerator;
import zen.parser.ZToken;
import zen.parser.ZTypeChecker;

public class DShellCommandNode extends ZSugarNode {
	private final ArrayList<ZNode> ArgList;
	public ZNode PipedNextNode;

	public DShellCommandNode(ZNode ParentNode, ZToken Token, String Command) {
		super(ParentNode, Token, 0);
		this.PipedNextNode = null;
		this.ArgList = new ArrayList<ZNode>();
		this.AppendArgNode(new DShellArgNode(ParentNode, Command));
	}

	public void AppendArgNode(ZNode Node) {
		this.ArgList.add(this.SetChild(Node, true));
	}

	public ZNode AppendPipedNextNode(DShellCommandNode Node) {
		DShellCommandNode CurrentNode = this;
		while(CurrentNode.PipedNextNode != null) {
			CurrentNode = (DShellCommandNode) CurrentNode.PipedNextNode;
		}
		CurrentNode.PipedNextNode = CurrentNode.SetChild(Node, false);
		return this;
	}

	public int GetArgSize() {
		return this.ArgList.size();
	}

	public void SetArgAt(int Index, ZNode ArgNode) {
		this.ArgList.set(Index, ArgNode);
	}

	public ZNode GetArgAt(int Index) {
		return this.ArgList.get(Index);
	}

	@Override
	public ZDesugarNode DeSugar(ZGenerator Generator, ZTypeChecker TypeChekcer) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}
}