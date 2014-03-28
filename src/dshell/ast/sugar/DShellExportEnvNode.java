package dshell.ast.sugar;

import libbun.parser.ast.ZFuncCallNode;
import libbun.parser.ast.ZGetNameNode;
import libbun.parser.ast.ZLetVarNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.parser.ast.ZDesugarNode;
import libbun.parser.ast.ZSugarNode;
import libbun.parser.ZGenerator;
import libbun.parser.ZTypeChecker;

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

		ZNode LetNode = new ZLetVarNode(this, ZLetVarNode._IsReadOnly);
		LetNode.SetNode(ZLetVarNode._NameInfo, this.AST[_NameInfo]);
		ZNode FuncCallNode = new ZFuncCallNode(LetNode, new ZGetNameNode(LetNode, null, "getEnv"));
		FuncCallNode.SetNode(ZNode._AppendIndex, new ZStringNode(FuncCallNode, null, envName));
		LetNode.SetNode(ZLetVarNode._InitValue, FuncCallNode);
		return new ZDesugarNode(this, new ZNode[]{SetEnvNode, LetNode});
	}
}
