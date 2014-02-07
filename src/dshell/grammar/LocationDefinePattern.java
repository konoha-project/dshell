package dshell.grammar;

import dshell.ast.DShellDummyNode;
import zen.ast.ZGetNameNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZTokenContext;

public class LocationDefinePattern extends ZMatchFunction {
	private LocationPattern locationPattern;
	public LocationDefinePattern() {
		this.locationPattern = new LocationPattern();
	}

	@Override	//TODO: multiple host, ssh
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		TokenContext.MoveNext();
		ZNode Node = TokenContext.ParsePattern(ParentNode, "$Name$", ZTokenContext.Required);
		if(!Node.IsErrorNode() && TokenContext.MatchToken("=")) {
			ZNode ValueNode = TokenContext.ParsePattern(ParentNode, "$StringLiteral$", ZTokenContext.Required);
			if(!ValueNode.IsErrorNode()) {
				String NameSymbol = ((ZGetNameNode)Node).VarName;
				ParentNode.GetNameSpace().DefineExpression(NameSymbol, this.locationPattern);
				ParentNode.GetNameSpace().SetGlobalSymbol(NameSymbol, (ZStringNode)ValueNode);
				return new DShellDummyNode(ParentNode);
			}
		}
		return null;
	}

}
