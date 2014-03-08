package dshell.grammar;

import zen.ast.ZFuncCallNode;
import zen.ast.ZGetNameNode;
import zen.ast.ZLetNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.util.ZMatchFunction;
import zen.parser.ZTokenContext;

public class ImportEnvPattern extends ZMatchFunction {
	public final static String PatternName = "$ImportEnv$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		ZNode LetNode = new ZLetNode(ParentNode);
		LetNode = TokenContext.MatchToken(LetNode, "env", ZTokenContext._Required);
		LetNode = TokenContext.MatchPattern(LetNode, ZNode._NameInfo, "$Name$", ZTokenContext._Required);
		LetNode.Set(ZNode._TypeInfo, ParentNode.GetNameSpace().GetTypeNode("String", null));
		String Name = ((ZLetNode)LetNode).Symbol;
		ZNode FuncCallNode = new ZFuncCallNode(LetNode, new ZGetNameNode(LetNode, null, "getEnv"));
		FuncCallNode.Set(ZNode._AppendIndex, new ZStringNode(FuncCallNode, null, Name));
		LetNode.Set(ZLetNode._InitValue, FuncCallNode);
		return LetNode;
	}
}
