package dshell.ast;

import dshell.lang.ModifiedTypeSafer;
import zen.ast.ZFuncCallNode;
import zen.ast.ZGetNameNode;
import zen.ast.ZLetNode;
import zen.ast.ZListNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.codegen.jvm.ModifiedAsmGenerator;
import zen.codegen.jvm.ModifiedJavaEngine;
import zen.parser.ZToken;
import zen.parser.ZVisitor;

public class DShellExportEnvNode extends ZListNode {
	public final static int _EXPORT = 0;
	public final static int _LET = 1;
	private String envName;

	public DShellExportEnvNode(ZNode ParentNode) {
		super(ParentNode, null, 2);
	}

	@Override public void SetNameInfo(ZToken NameToken, String Name) {
		this.envName = Name;
		ZNode Node = new ZLetNode(this);
		Node.SetNameInfo(NameToken, Name);
		Node.Set(ZNode._TypeInfo, ParentNode.GetNameSpace().GetTypeNode("String", null));
		ZNode FuncCallNode = new ZFuncCallNode(Node, new ZGetNameNode(Node, null, "getEnv"));
		FuncCallNode.Set(ZNode._AppendIndex, new ZStringNode(FuncCallNode, null, Name));
		Node.Set(ZLetNode._InitValue, FuncCallNode);
		this.Set(_LET, Node);
	}

	public ZNode SetEnvValue(ZNode Node) {
		if(Node.IsErrorNode()) {
			return Node;
		}
		ZNode FuncCallNode = new ZFuncCallNode(Node, new ZGetNameNode(Node, null, "setEnv"));
		FuncCallNode.Set(ZNode._AppendIndex, new ZStringNode(FuncCallNode, null, this.envName));
		FuncCallNode.Set(ZNode._AppendIndex, Node);
		this.Set(_EXPORT, FuncCallNode);
		return this;
	}

	@Override public void Accept(ZVisitor Visitor) {
		if(Visitor instanceof ModifiedTypeSafer) {
			((ModifiedTypeSafer)Visitor).VisitExportEnvNode(this);
		}
		else if(Visitor instanceof ModifiedAsmGenerator) {
			((ModifiedAsmGenerator)Visitor).VisitExportEnvNode(this);
		}
		else if(Visitor instanceof ModifiedJavaEngine) {
			((ModifiedJavaEngine)Visitor).VisitExportEnvNode(this);
		}
		else {
			throw new RuntimeException(Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
