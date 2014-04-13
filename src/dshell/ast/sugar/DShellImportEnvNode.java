package dshell.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.literal.BunStringNode;
import libbun.encode.AbstractGenerator;
import libbun.parser.BTypeChecker;

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
		GetNameNode FuncNameNode = new GetNameNode(this.ParentNode, this.SourceToken, "getEnv");
		FuncCallNode GetEnvNode = new FuncCallNode(this.ParentNode, FuncNameNode);
		GetEnvNode.SourceToken = SourceToken;
		GetEnvNode.SetNode(BNode._AppendIndex, new BunStringNode(GetEnvNode, null, EnvName));
		LetNode.SetNode(BunLetVarNode._InitValue, GetEnvNode);
		return new DesugarNode(this, LetNode);
	}
}
