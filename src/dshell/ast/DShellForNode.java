package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import zen.ast.ZBlockNode;
import zen.ast.ZNode;
import zen.ast.ZVarNode;
import zen.parser.ZVisitor;
import zen.type.ZType;

public class DShellForNode extends ZNode {
	public final static int _InitValue = 0;
	public final static int _Init  = 1;	// VarNode
	public final static int _Cond  = 2;
	public final static int _Next  = 3;
	public final static int _Block = 4;

	private ZType GivenType = null;
	private String GivenName = null;

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
		return this.AST[_Init] != null && this.AST[_Init] instanceof ZVarNode;
	}

	public final ZType DeclType() {
		if(this.GivenType == null) {
			this.GivenType = ((ZVarNode)this.AST[_Init]).DeclType();
		}
		return this.GivenType;
	}

	public final void SetDeclType(ZType Type) {
		this.GivenType = Type;
	}

	public final String GetName() {
		if(this.GivenName == null) {
			this.GivenName = ((ZVarNode)this.AST[_Init]).GetName();
		}
		return this.GivenName;
	}

	public final ZNode InitValueNode() {
		if(this.AST[_InitValue] == null) {
			this.AST[_InitValue] = ((ZVarNode)this.AST[_Init]).InitValueNode();
		}
		return this.AST[_InitValue];
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
			this.InitValueNode();
		}
		this.CondNode();
		if(this.HasNextNode()) {
			this.NextNode();
		}
	}
}
