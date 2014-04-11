package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.parser.BVisitor;

public class DShellForNode extends BNode {
	public final static int _VarDecl = 0;	// LetVarNode
	public final static int _Init  = 1;	// VarBlockNode
	public final static int _Cond  = 2;
	public final static int _Next  = 3;
	public final static int _Block = 4;

	public DShellForNode(BNode ParentNode) {
		super(ParentNode, null, 5);
	}

	@Override
	public void Accept(BVisitor Visitor) {
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitForNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}

	public final boolean HasDeclNode() {
		return this.AST[_Init] != null;
	}

	public final BunLetVarNode VarDeclNode() {
		if(this.AST[_VarDecl] == null) {
			BNode Node = this.AST[_Init];
			if(Node instanceof BunLetVarNode) {
				this.AST[_VarDecl] = Node;
			}
			else if(Node instanceof BunVarBlockNode) {
				this.AST[_VarDecl] = ((BunVarBlockNode)this.AST[_Init]).VarDeclNode();
			}
			else {
				Utils.fatal(1, "invalid type: " + Node.getClass().getSimpleName());
			}
		}
		return (BunLetVarNode) this.AST[_VarDecl];
	}

	public final BNode CondNode() {
		BNode CondNode = this.AST[_Cond];
		if(!(CondNode.ParentNode instanceof BunBlockNode)) {
			CondNode.ParentNode = this.BlockNode();
		}
		return CondNode;
	}

	public final boolean HasNextNode() {
		return this.NextNode() != null;
	}

	public final BNode NextNode() {
		BNode NextNode = this.AST[_Next];
		if(!(NextNode.ParentNode instanceof BunBlockNode)) {
			NextNode.ParentNode = this.BlockNode();
		}
		return NextNode;
	}

	public final BunBlockNode BlockNode() {
		BNode BlockNode = this.AST[_Block];
		if(BlockNode instanceof BunBlockNode) {
			return (BunBlockNode) BlockNode;
		}
		Utils.fatal(1, "need BlockNode");
		return null;
	}

	public final void PrepareTypeCheck() { // must call before type check
		if(this.HasDeclNode()) {
			this.VarDeclNode();
		}
		this.CondNode();
		if(this.HasNextNode()) {
			this.NextNode();
		}
	}
}
