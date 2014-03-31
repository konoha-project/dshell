package dshell.ast.sugar;

import libbun.parser.ZGenerator;
import libbun.parser.ZMacroFunc;
import libbun.parser.ZTypeChecker;
import libbun.parser.ast.ZDesugarNode;
import libbun.parser.ast.ZLetVarNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.parser.ast.ZSugarNode;
import libbun.type.ZType;

public class DShellImportEnvNode extends ZSugarNode {
	public final static int _NameInfo = 0;

	public DShellImportEnvNode(ZNode ParentNode) {
		super(ParentNode, null, 1);
	}

	@Override
	public ZDesugarNode DeSugar(ZGenerator Generator, ZTypeChecker TypeChecker) {
		String EnvName = this.AST[_NameInfo].SourceToken.GetText();
		ZNode LetNode = new ZLetVarNode(this, ZLetVarNode._IsReadOnly);
		LetNode.SetNode(ZLetVarNode._NameInfo, this.AST[_NameInfo]);
		ZMacroFunc GetEnvFunc = Generator.GetMacroFunc("getEnv", ZType.StringType, 1);
		ZNode GetEnvNode = TypeChecker.CreateDefinedFuncCallNode(this.ParentNode, this.SourceToken, GetEnvFunc);
		GetEnvNode.SetNode(ZNode._AppendIndex, new ZStringNode(GetEnvNode, null, EnvName));
		LetNode.SetNode(ZLetVarNode._InitValue, GetEnvNode);
		return new ZDesugarNode(this, LetNode);
	}

}
