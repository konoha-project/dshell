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

public class DShellImportEnvNode extends BSugarNode {
	public final static int _NameInfo = 0;

	public DShellImportEnvNode(BNode ParentNode) {
		super(ParentNode, null, 1);
	}

	@Override
	public BDesugarNode DeSugar(BGenerator Generator, BTypeChecker TypeChecker) {
		String EnvName = this.AST[_NameInfo].SourceToken.GetText();
		BNode LetNode = new BLetVarNode(this, BLetVarNode._IsReadOnly, null, null);
		LetNode.SetNode(BLetVarNode._NameInfo, this.AST[_NameInfo]);
		BMacroFunc GetEnvFunc = Generator.GetMacroFunc("getEnv", BType.StringType, 1);
		BNode GetEnvNode = TypeChecker.CreateDefinedFuncCallNode(this.ParentNode, this.SourceToken, GetEnvFunc);
		GetEnvNode.SetNode(BNode._AppendIndex, new BStringNode(GetEnvNode, null, EnvName));
		LetNode.SetNode(BLetVarNode._InitValue, GetEnvNode);
		return new BDesugarNode(this, LetNode);
	}

}
