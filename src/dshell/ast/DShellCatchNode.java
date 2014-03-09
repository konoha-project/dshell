package dshell.ast;

import dshell.lang.ModifiedTypeSafer;
import dshell.lib.Utils;
import zen.ast.ZBlockNode;
import zen.ast.ZNode;
import zen.codegen.jvm.ModifiedAsmGenerator;
import zen.codegen.jvm.ModifiedJavaEngine;
import zen.parser.ZToken;
import zen.parser.ZVisitor;
import zen.type.ZType;

public class DShellCatchNode extends ZNode {
	public final static int _Block = 0;

	public ZType   ExceptionType = ZType.VarType;
	public String  ExceptionName = null;

	public ZToken NameToken = null;

	public DShellCatchNode(ZNode ParentNode) {
		super(ParentNode, null, 1);
	}

	@Override public void SetTypeInfo(ZToken TypeToken, ZType Type) {
		this.ExceptionType = Type;
	}
	@Override public void SetNameInfo(ZToken NameToken, String Name) {
		this.ExceptionName = Name;
		this.NameToken = NameToken;
	}

	public final ZBlockNode CatchBlockNode() {
		ZNode BlockNode = this.AST[_Block];
		if(BlockNode instanceof ZBlockNode) {
			return (ZBlockNode) BlockNode;
		}
		Utils.fatal(1, "need ZBlockNode: " + BlockNode);
		return null;
	}

	@Override public void Accept(ZVisitor Visitor) {
		if(Visitor instanceof ModifiedTypeSafer) {
			((ModifiedTypeSafer)Visitor).VisitCatchNode(this);
		}
		else if(Visitor instanceof ModifiedAsmGenerator) {
			((ModifiedAsmGenerator)Visitor).VisitCatchNode(this);
		}
		else if(Visitor instanceof ModifiedJavaEngine) {
			((ModifiedJavaEngine)Visitor).VisitCatchNode(this);
		}
		else {
			throw new RuntimeException(Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
