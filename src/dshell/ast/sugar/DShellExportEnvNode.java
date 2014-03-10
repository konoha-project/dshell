package dshell.ast.sugar;

import zen.ast.ZFuncCallNode;
import zen.ast.ZGetNameNode;
import zen.ast.ZLetNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.ast.sugar.ZDesugarNode;
import zen.ast.sugar.ZSyntaxSugarNode;
import zen.parser.ZGenerator;
import zen.parser.ZToken;

public class DShellExportEnvNode extends ZSyntaxSugarNode {
	public final static int _EXPR = 0;

	private String envName;
	private ZToken envNameToken;

	public DShellExportEnvNode(ZNode ParentNode) {
		super(ParentNode, null, 1);
	}

	@Override public void SetNameInfo(ZToken NameToken, String Name) {
		this.envNameToken = NameToken;
		this.envName = Name;
	}

	@Override
	public ZDesugarNode DeSugar(ZGenerator Generator) {
		ZNode SetEnvNode = new ZFuncCallNode(this, new ZGetNameNode(this, null, "setEnv"));
		SetEnvNode.Set(ZNode._AppendIndex, new ZStringNode(SetEnvNode, null, this.envName));
		SetEnvNode.Set(ZNode._AppendIndex, this.AST[DShellExportEnvNode._EXPR]);

		ZNode LetNode = new ZLetNode(this);
		LetNode.SetNameInfo(this.envNameToken, this.envName);
		LetNode.Set(ZNode._TypeInfo, ParentNode.GetNameSpace().GetTypeNode("String", null));
		ZNode FuncCallNode = new ZFuncCallNode(LetNode, new ZGetNameNode(LetNode, null, "getEnv"));
		FuncCallNode.Set(ZNode._AppendIndex, new ZStringNode(FuncCallNode, null, this.envName));
		LetNode.Set(ZLetNode._InitValue, FuncCallNode);
		return new ZDesugarNode(this, new ZNode[]{SetEnvNode, LetNode});
	}
}
