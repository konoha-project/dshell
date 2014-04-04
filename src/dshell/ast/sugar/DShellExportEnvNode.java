package dshell.ast.sugar;

import libbun.ast.BDesugarNode;
import libbun.ast.BNode;
import libbun.ast.BSugarNode;
import libbun.ast.decl.BLetVarNode;
import libbun.ast.literal.BStringNode;
import libbun.parser.BGenerator;
import libbun.parser.BTypeChecker;
import libbun.type.BMacroFunc;
import libbun.type.BType;

public class DShellExportEnvNode extends BSugarNode {
	public final static int _NameInfo = 0;
	public final static int _Expr = 1;

	public DShellExportEnvNode(BNode ParentNode) {
		super(ParentNode, null, 2);
	}

	@Override
	public BDesugarNode DeSugar(BGenerator Generator, BTypeChecker TypeChecker) {
		String EnvName = this.AST[_NameInfo].SourceToken.GetText();
		BMacroFunc SetEnvFunc = Generator.GetMacroFunc("setEnv", BType.StringType, 2);
		BNode SetEnvNode = TypeChecker.CreateDefinedFuncCallNode(this.ParentNode, this.SourceToken, SetEnvFunc);
		SetEnvNode.SetNode(BNode._AppendIndex, new BStringNode(SetEnvNode, null, EnvName));
		SetEnvNode.SetNode(BNode._AppendIndex, this.AST[DShellExportEnvNode._Expr]);

		BNode LetNode = new BLetVarNode(this, BLetVarNode._IsReadOnly, null, null);
		LetNode.SetNode(BLetVarNode._NameInfo, this.AST[_NameInfo]);
		BMacroFunc GetEnvFunc = Generator.GetMacroFunc("getEnv", BType.StringType, 1);
		BNode GetEnvNode = TypeChecker.CreateDefinedFuncCallNode(this.ParentNode, this.SourceToken, GetEnvFunc);
		GetEnvNode.SetNode(BNode._AppendIndex, new BStringNode(GetEnvNode, null, EnvName));
		LetNode.SetNode(BLetVarNode._InitValue, GetEnvNode);
		return new BDesugarNode(this, new BNode[]{SetEnvNode, LetNode});
	}
}
