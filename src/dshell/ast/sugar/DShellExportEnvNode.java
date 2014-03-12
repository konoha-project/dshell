package dshell.ast.sugar;

import zen.ast.ZFuncCallNode;
import zen.ast.ZGetNameNode;
import zen.ast.ZLetNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.ast.ZDesugarNode;
import zen.ast.ZSugarNode;
import zen.parser.ZGenerator;
import zen.parser.ZTypeChecker;

public class DShellExportEnvNode extends ZSugarNode {
	public final static int _NameInfo = 0;
	public final static int _EXPR = 1;

	public DShellExportEnvNode(ZNode ParentNode) {
		super(ParentNode, null, 2);
	}

	@Override
	public ZDesugarNode DeSugar(ZGenerator Generator, ZTypeChecker TypeChekcer) {
		String envName = this.AST[_NameInfo].SourceToken.GetText();
		ZNode SetEnvNode = new ZFuncCallNode(this, new ZGetNameNode(this, null, "setEnv"));
		SetEnvNode.SetNode(ZNode._AppendIndex, new ZStringNode(SetEnvNode, null, envName));
		SetEnvNode.SetNode(ZNode._AppendIndex, this.AST[DShellExportEnvNode._EXPR]);

		ZNode LetNode = new ZLetNode(this);
		LetNode.SetNode(ZLetNode._NameInfo, this.AST[_NameInfo]);
		LetNode.SetNode(ZLetNode._TypeInfo, ParentNode.GetNameSpace().GetTypeNode("String", null));
		ZNode FuncCallNode = new ZFuncCallNode(LetNode, new ZGetNameNode(LetNode, null, "getEnv"));
		FuncCallNode.SetNode(ZNode._AppendIndex, new ZStringNode(FuncCallNode, null, envName));
		LetNode.SetNode(ZLetNode._InitValue, FuncCallNode);
		return new ZDesugarNode(this, new ZNode[]{SetEnvNode, LetNode});
	}
}
