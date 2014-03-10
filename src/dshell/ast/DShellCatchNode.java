package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import zen.ast.ZBlockNode;
import zen.ast.ZNode;
import zen.ast.ZTypeNode;
import zen.parser.ZVisitor;
import zen.type.ZType;

public class DShellCatchNode extends ZNode {
	public final static int _NameInfo = 0;
	public final static int _TypeInfo = 1;
	public final static int _Block = 2;

	private String ExceptionName = null;
	private ZType ExceptionType = null;

	public DShellCatchNode(ZNode ParentNode) {
		super(ParentNode, null, 3);
	}

	public final String ExceptionName() {
		if(this.ExceptionName == null) {
			this.ExceptionName = this.AST[_NameInfo].SourceToken.GetText();
		}
		return this.ExceptionName;
	}

	public final ZType ExceptionType() {
		if(this.ExceptionType == null) {
			this.ExceptionType = ((ZTypeNode) this.AST[_TypeInfo]).Type;
		}
		return this.ExceptionType;
	}

	public final ZBlockNode BlockNode() {
		ZNode BlockNode = this.AST[_Block];
		if(BlockNode instanceof ZBlockNode) {
			return (ZBlockNode) BlockNode;
		}
		Utils.fatal(1, "need ZBlockNode: " + BlockNode);
		return null;
	}

	public void SetExceptionType(ZType Type) {
		this.ExceptionType = Type;
	}

	@Override public void Accept(ZVisitor Visitor) {
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitCatchNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
