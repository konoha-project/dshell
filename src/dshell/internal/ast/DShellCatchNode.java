package dshell.internal.ast;

import dshell.internal.jvm.JavaTypeTable;
import dshell.internal.lang.DShellVisitor;
import dshell.internal.lib.Utils;
import dshell.lang.Exception;
import libbun.ast.BunBlockNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.literal.BunTypeNode;
import libbun.ast.BNode;
import libbun.parser.classic.LibBunVisitor;
import libbun.type.BType;

public class DShellCatchNode extends BNode {
	public final static int _NameInfo = 0;
	public final static int _TypeInfo = 1;
	public final static int _Block = 2;

	private String exceptionName = null;
	private BType exceptionType = null;

	public DShellCatchNode(BNode parentNode) {
		super(parentNode, 3);
	}

	public final String exceptionName() {
		if(this.exceptionName == null) {
			this.exceptionName = this.AST[_NameInfo].SourceToken.GetText();
		}
		return this.exceptionName;
	}

	public final BType exceptionType() {
		if(this.exceptionType == null && this.hasTypeInfo()) {
			this.exceptionType = ((BunTypeNode) this.AST[_TypeInfo]).Type;
		}
		return this.exceptionType;
	}

	public void setExceptionType(BType type) {
		this.exceptionType = type;
	}

	public void setDefaultType(JavaTypeTable typeTable) {
		if(this.exceptionType() == null) {
			this.exceptionType = typeTable.GetBunType(Exception.class);
		}
	}

	public BunLetVarNode toLetVarNode() {
		BunLetVarNode node = new BunLetVarNode(this.ParentNode, BunLetVarNode._IsReadOnly, null, null);
		node.SetNode(BunLetVarNode._NameInfo, this.AST[_NameInfo]);
		if(this.hasTypeInfo()) {
			node.SetNode(BunLetVarNode._TypeInfo, this.AST[_TypeInfo]);
		}
		node.SetDeclType(this.exceptionType());
		return node;
	}

	public final BunBlockNode blockNode() {
		BNode blockNode = this.AST[_Block];
		if(blockNode instanceof BunBlockNode) {
			return (BunBlockNode) blockNode;
		}
		Utils.fatal(1, "need ZBlockNode: " + blockNode);
		return null;
	}

	public boolean hasTypeInfo() {
		return this.AST[_TypeInfo] != null;
	}

	@Override public void Accept(LibBunVisitor visitor) {
		if(visitor instanceof DShellVisitor) {
			((DShellVisitor)visitor).visitCatchNode(this);
		}
		else {
			Utils.fatal(1, visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
