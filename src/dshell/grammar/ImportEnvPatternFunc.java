package dshell.grammar;

import libbun.parser.ast.ZFuncCallNode;
import libbun.parser.ast.ZGetNameNode;
import libbun.parser.ast.ZLetVarNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.util.ZMatchFunction;
import libbun.parser.ZTokenContext;

public class ImportEnvPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$ImportEnv$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode LetNode = new ZLetVarNode(ParentNode, ZLetVarNode._IsReadOnly);
		LetNode = TokenContext.MatchToken(LetNode, "env", ZTokenContext._Required);
		LetNode = TokenContext.MatchPattern(LetNode, ZLetVarNode._NameInfo, "$Name$", ZTokenContext._Required);
		String Name = ((ZLetVarNode)LetNode).GetName();
		ZNode FuncCallNode = new ZFuncCallNode(LetNode, new ZGetNameNode(LetNode, null, "getEnv"));
		FuncCallNode.SetNode(ZNode._AppendIndex, new ZStringNode(FuncCallNode, null, Name));
		LetNode.SetNode(ZLetVarNode._InitValue, FuncCallNode);
		return LetNode;
	}
}
