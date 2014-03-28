package dshell.grammar;

import libbun.parser.ast.ZEmptyNode;
import libbun.parser.ast.ZGetNameNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.util.ZMatchFunction;
import libbun.parser.ZTokenContext;

/*
 * location www = hoge@192.168.12.3, huge@192.168.2.4:567
 * */
public class LocationDefinePatternFunc extends ZMatchFunction {
	private final LocationPatternFunc locationPattern;
	public LocationDefinePatternFunc() {
		this.locationPattern = new LocationPatternFunc();
	}

	@Override	//TODO: multiple host, ssh
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		TokenContext.MoveNext();
		ZNode Node = TokenContext.ParsePattern(ParentNode, "$Name$", ZTokenContext._Required);
		if(!Node.IsErrorNode() && TokenContext.MatchToken("=")) {
			ZNode ValueNode = TokenContext.ParsePattern(ParentNode, "$StringLiteral$", ZTokenContext._Required);
			if(!ValueNode.IsErrorNode()) {
				String NameSymbol = ((ZGetNameNode)Node).GetName();
				ParentNode.GetNameSpace().DefineExpression(NameSymbol, this.locationPattern);
				ParentNode.GetNameSpace().SetSymbol(NameSymbol, (ZStringNode)ValueNode);
				return new ZEmptyNode(ParentNode, null);
			}
		}
		return null;
	}

}
