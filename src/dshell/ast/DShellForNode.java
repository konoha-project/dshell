package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.parser.ast.ZBlockNode;
import libbun.parser.ast.ZLetVarNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZVarBlockNode;
import libbun.parser.ZVisitor;

public class DShellForNode extends ZNode {
	public final static int _VarDecl = 0;	// LetVarNode
	public final static int _Init  = 1;	// VarBlockNode
	public final static int _Cond  = 2;
	public final static int _Next  = 3;
	public final static int _Block = 4;

	public DShellForNode(ZNode ParentNode) {
		super(ParentNode, null, 5);
	}

	@Override
	public void Accept(ZVisitor Visitor) {
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

	public final ZLetVarNode VarDeclNode() {
		if(this.AST[_VarDecl] == null) {
			this.AST[_VarDecl] = ((ZVarBlockNode)this.AST[_Init]).VarDeclNode();
		}
		return (ZLetVarNode) this.AST[_VarDecl];
	}

	public final ZNode CondNode() {
		ZNode CondNode = this.AST[_Cond];
		if(!(CondNode.ParentNode instanceof ZBlockNode)) {
			CondNode.ParentNode = this.BlockNode();
		}
		return CondNode;
	}

	public final boolean HasNextNode() {
		return this.NextNode() != null;
	}

	public final ZNode NextNode() {
		ZNode NextNode = this.AST[_Next];
		if(!(NextNode.ParentNode instanceof ZBlockNode)) {
			NextNode.ParentNode = this.BlockNode();
		}
		return NextNode;
	}

	public final ZBlockNode BlockNode() {
		ZNode BlockNode = this.AST[_Block];
		if(BlockNode instanceof ZBlockNode) {
			return (ZBlockNode) BlockNode;
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
