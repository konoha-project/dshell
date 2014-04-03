package dshell.ast.sugar;

import libbun.parser.ast.ZLetVarNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.parser.ast.ZDesugarNode;
import libbun.parser.ast.ZSugarNode;
import libbun.parser.ZGenerator;
import libbun.parser.ZMacroFunc;
import libbun.parser.ZTypeChecker;
import libbun.type.ZType;

public class DShellExportEnvNode extends ZSugarNode {
	public final static int _NameInfo = 0;
	public final static int _Expr = 1;

	public DShellExportEnvNode(ZNode ParentNode) {
		super(ParentNode, null, 2);
	}

	@Override
	public ZDesugarNode DeSugar(ZGenerator Generator, ZTypeChecker TypeChecker) {
		String EnvName = this.AST[_NameInfo].SourceToken.GetText();
		ZMacroFunc SetEnvFunc = Generator.GetMacroFunc("setEnv", ZType.StringType, 2);
		ZNode SetEnvNode = TypeChecker.CreateDefinedFuncCallNode(this.ParentNode, this.SourceToken, SetEnvFunc);
		SetEnvNode.SetNode(ZNode._AppendIndex, new ZStringNode(SetEnvNode, null, EnvName));
		SetEnvNode.SetNode(ZNode._AppendIndex, this.AST[DShellExportEnvNode._Expr]);

		ZNode LetNode = new ZLetVarNode(this, ZLetVarNode._IsReadOnly);
		LetNode.SetNode(ZLetVarNode._NameInfo, this.AST[_NameInfo]);
		ZMacroFunc GetEnvFunc = Generator.GetMacroFunc("getEnv", ZType.StringType, 1);
		ZNode GetEnvNode = TypeChecker.CreateDefinedFuncCallNode(this.ParentNode, this.SourceToken, GetEnvFunc);
		GetEnvNode.SetNode(ZNode._AppendIndex, new ZStringNode(GetEnvNode, null, EnvName));
		LetNode.SetNode(ZLetVarNode._InitValue, GetEnvNode);
		return new ZDesugarNode(this, new ZNode[]{SetEnvNode, LetNode});
	}
}
