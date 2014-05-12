package dshell.ast;

import dshell.ast.sugar.ArgumentNode;
import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BNode;
import libbun.parser.classic.BToken;
import libbun.parser.classic.LibBunVisitor;
import libbun.type.BType;
import libbun.util.BArray;

public class CommandNode extends BNode {
	private final BArray<BNode> argList;
	private BType retType = BType.VarType;
	private CommandNode pipedNextNode;

	public CommandNode(BNode parentNode, BToken token, String command) {
		super(parentNode, 0);
		this.SourceToken = token;
		this.pipedNextNode = null;
		this.argList = new BArray<BNode>(new BNode[]{});
		this.appendArgNode(new ArgumentNode(parentNode, command));
	}

	public void appendArgNode(BNode node) {
		this.argList.add(this.SetChild(node, true));
	}

	public BNode appendPipedNextNode(CommandNode node) {
		CommandNode currentNode = this;
		while(currentNode.pipedNextNode != null) {
			currentNode = currentNode.pipedNextNode;
		}
		currentNode.pipedNextNode = (CommandNode) currentNode.SetChild(node, false);
		return this;
	}

	public int getArgSize() {
		return this.argList.size();
	}

	public void setArgAt(int index, BNode argNode) {
		BArray.SetIndex(this.argList, index, argNode);
	}

	public BNode getArgAt(int index) {
		return BArray.GetIndex(this.argList, index);
	}

	public void setType(BType type) {
		this.retType = type;
	}

	public BType retType() {
		return this.retType;
	}

	public CommandNode getPipedNextNode() {
		return this.pipedNextNode;
	}

	public void setPipedNextNode(CommandNode node) {
		this.pipedNextNode = node;
	}
	@Override
	public void Accept(LibBunVisitor visitor) {
		if(visitor instanceof DShellVisitor) {
			((DShellVisitor)visitor).visitCommandNode(this);
		}
		else {
			Utils.fatal(1, visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}