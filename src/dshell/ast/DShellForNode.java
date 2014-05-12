package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.parser.classic.LibBunVisitor;

public class DShellForNode extends BNode {
	public final static int _VarDecl = 0;	// LetVarNode
	public final static int _Init  = 1;	// VarBlockNode
	public final static int _Cond  = 2;
	public final static int _Next  = 3;
	public final static int _Block = 4;

	public DShellForNode(BNode parentNode) {
		super(parentNode, 5);
	}

	@Override
	public void Accept(LibBunVisitor visitor) {
		if(visitor instanceof DShellVisitor) {
			((DShellVisitor)visitor).visitForNode(this);
		}
		else {
			Utils.fatal(1, visitor.getClass().getName() + " is unsupported Visitor");
		}
	}

	public final boolean hasDeclNode() {
		return this.AST[_Init] != null;
	}

	public final BunLetVarNode toVarDeclNode() {
		if(this.AST[_VarDecl] == null) {
			BNode node = this.AST[_Init];
			if(node instanceof BunLetVarNode) {
				this.AST[_VarDecl] = node;
			}
			else if(node instanceof BunVarBlockNode) {
				this.AST[_VarDecl] = ((BunVarBlockNode)this.AST[_Init]).VarDeclNode();
			}
			else {
				Utils.fatal(1, "invalid type: " + node.getClass().getSimpleName());
			}
		}
		return (BunLetVarNode) this.AST[_VarDecl];
	}

	public final BNode condNode() {
		BNode condNode = this.AST[_Cond];
		if(!(condNode.ParentNode instanceof BunBlockNode)) {
			condNode.ParentNode = this.blockNode();
		}
		return condNode;
	}

	public final boolean hasNextNode() {
		return this.nextNode() != null;
	}

	public final BNode nextNode() {
		BNode nextNode = this.AST[_Next];
		if(!(nextNode.ParentNode instanceof BunBlockNode)) {
			nextNode.ParentNode = this.blockNode();
		}
		return nextNode;
	}

	public final BunBlockNode blockNode() {
		BNode blockNode = this.AST[_Block];
		if(blockNode instanceof BunBlockNode) {
			return (BunBlockNode) blockNode;
		}
		Utils.fatal(1, "need BlockNode");
		return null;
	}

	public final void prepareTypeCheck() { // must call before type check
		if(this.hasDeclNode()) {
			this.toVarDeclNode();
		}
		this.condNode();
		if(this.hasNextNode()) {
			this.nextNode();
		}
	}
}
