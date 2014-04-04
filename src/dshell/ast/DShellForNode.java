package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BNode;
import libbun.ast.BBlockNode;
import libbun.ast.decl.ZVarBlockNode;
import libbun.ast.decl.BLetVarNode;
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
		return this.AST[_Init] != null && this.AST[_Init] instanceof ZVarBlockNode;
	}

	public final BLetVarNode VarDeclNode() {
		if(this.AST[_VarDecl] == null) {
			this.AST[_VarDecl] = ((ZVarBlockNode)this.AST[_Init]).VarDeclNode();
		}
		return (BLetVarNode) this.AST[_VarDecl];
	}

	public final BNode CondNode() {
		BNode CondNode = this.AST[_Cond];
		if(!(CondNode.ParentNode instanceof BBlockNode)) {
			CondNode.ParentNode = this.BlockNode();
		}
		return CondNode;
	}

	public final boolean HasNextNode() {
		return this.NextNode() != null;
	}

	public final BNode NextNode() {
		BNode NextNode = this.AST[_Next];
		if(!(NextNode.ParentNode instanceof BBlockNode)) {
			NextNode.ParentNode = this.BlockNode();
		}
		return NextNode;
	}

	public final BBlockNode BlockNode() {
		BNode BlockNode = this.AST[_Block];
		if(BlockNode instanceof BBlockNode) {
			return (BBlockNode) BlockNode;
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
