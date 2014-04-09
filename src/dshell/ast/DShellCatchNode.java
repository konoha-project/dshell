package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BunBlockNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.BNode;
import libbun.ast.literal.BunTypeNode;
import libbun.parser.BVisitor;
import libbun.type.BType;

public class DShellCatchNode extends BNode {
	public final static int _NameInfo = 0;
	public final static int _TypeInfo = 1;
	public final static int _Block = 2;

	private String ExceptionName = null;
	private BType ExceptionType = null;

	public DShellCatchNode(BNode ParentNode) {
		super(ParentNode, null, 3);
	}

	public final String ExceptionName() {
		if(this.ExceptionName == null) {
			this.ExceptionName = this.AST[_NameInfo].SourceToken.GetText();
		}
		return this.ExceptionName;
	}

	public final BType ExceptionType() {
		if(this.ExceptionType == null) {
			this.ExceptionType = ((BunTypeNode) this.AST[_TypeInfo]).Type;
		}
		return this.ExceptionType;
	}

	public void SetExceptionType(BType Type) {
		this.ExceptionType = Type;
	}

	public BunLetVarNode ToLetVarNode() {
		BunLetVarNode Node = new BunLetVarNode(this.ParentNode, BunLetVarNode._IsReadOnly, null, null);
		Node.SetNode(BunLetVarNode._NameInfo, this.AST[_NameInfo]);
		Node.SetNode(BunLetVarNode._TypeInfo, this.AST[_TypeInfo]);
		Node.SetDeclType(this.ExceptionType());
		return Node;
	}

	public final BunBlockNode BlockNode() {
		BNode BlockNode = this.AST[_Block];
		if(BlockNode instanceof BunBlockNode) {
			return (BunBlockNode) BlockNode;
		}
		Utils.fatal(1, "need ZBlockNode: " + BlockNode);
		return null;
	}

	@Override public void Accept(BVisitor Visitor) {
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitCatchNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
