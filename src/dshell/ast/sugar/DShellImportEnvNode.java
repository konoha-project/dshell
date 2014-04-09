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

public class DShellImportEnvNode extends SyntaxSugarNode {
	public final static int _NameInfo = 0;

	public DShellImportEnvNode(BNode ParentNode) {
		super(ParentNode, null, 1);
	}

	@Override
	public DesugarNode DeSugar(AbstractGenerator Generator, BTypeChecker TypeChecker) {
		String EnvName = this.AST[_NameInfo].SourceToken.GetText();
		BNode LetNode = new BunLetVarNode(this, BunLetVarNode._IsReadOnly, null, null);
		LetNode.SetNode(BunLetVarNode._NameInfo, this.AST[_NameInfo]);
		BMacroFunc GetEnvFunc = Generator.GetMacroFunc("getEnv", BType.StringType, 1);
		BNode GetEnvNode = TypeChecker.CreateDefinedFuncCallNode(this.ParentNode, this.SourceToken, GetEnvFunc);
		GetEnvNode.SetNode(BNode._AppendIndex, new BunStringNode(GetEnvNode, null, EnvName));
		LetNode.SetNode(BunLetVarNode._InitValue, GetEnvNode);
		return new DesugarNode(this, LetNode);
	}
}
