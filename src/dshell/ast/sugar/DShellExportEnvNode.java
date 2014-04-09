package dshell.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.literal.BunStringNode;
import libbun.encode.AbstractGenerator;
import libbun.parser.BTypeChecker;
import libbun.type.BMacroFunc;
import libbun.type.BType;

public class DShellExportEnvNode extends SyntaxSugarNode {
	public final static int _NameInfo = 0;
	public final static int _Expr = 1;

	public DShellExportEnvNode(BNode ParentNode) {
		super(ParentNode, null, 2);
	}

	@Override
	public DesugarNode DeSugar(AbstractGenerator Generator, BTypeChecker TypeChecker) {
		String EnvName = this.AST[_NameInfo].SourceToken.GetText();
		BMacroFunc SetEnvFunc = Generator.GetMacroFunc("setEnv", BType.StringType, 2);
		BNode SetEnvNode = TypeChecker.CreateDefinedFuncCallNode(this.ParentNode, this.SourceToken, SetEnvFunc);
		SetEnvNode.SetNode(BNode._AppendIndex, new BunStringNode(SetEnvNode, null, EnvName));
		SetEnvNode.SetNode(BNode._AppendIndex, this.AST[DShellExportEnvNode._Expr]);

		BNode LetNode = new BunLetVarNode(this, BunLetVarNode._IsReadOnly, null, null);
		LetNode.SetNode(BunLetVarNode._NameInfo, this.AST[_NameInfo]);
		BMacroFunc GetEnvFunc = Generator.GetMacroFunc("getEnv", BType.StringType, 1);
		BNode GetEnvNode = TypeChecker.CreateDefinedFuncCallNode(this.ParentNode, this.SourceToken, GetEnvFunc);
		GetEnvNode.SetNode(BNode._AppendIndex, new BunStringNode(GetEnvNode, null, EnvName));
		LetNode.SetNode(BunLetVarNode._InitValue, GetEnvNode);
		return new DesugarNode(this, new BNode[]{SetEnvNode, LetNode});
	}
}
