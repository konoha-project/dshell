package dshell.grammar;

import libbun.ast.BNode;
import libbun.ast.EmptyNode;
import libbun.parser.BTokenContext;
import libbun.util.BMatchFunction;

/*
 * location www = hoge@192.168.12.3, huge@192.168.2.4:567
 * */
public class LocationDefinePatternFunc extends BMatchFunction {	//TODO
	private final LocationPatternFunc locationPattern;
	public LocationDefinePatternFunc() {
		this.locationPattern = new LocationPatternFunc();
	}

	@Override	//TODO: multiple host, ssh
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		TokenContext.MoveNext();
		BNode Node = TokenContext.ParsePattern(ParentNode, "$Name$", BTokenContext._Required);
		if(!Node.IsErrorNode() && TokenContext.MatchToken("=")) {
			BNode ValueNode = TokenContext.ParsePattern(ParentNode, "$StringLiteral$", BTokenContext._Required);
			if(!ValueNode.IsErrorNode()) {
//				String NameSymbol = ((BGetNameNode)Node).GetName();
//				ParentNode.GetNameSpace().DefineExpression(NameSymbol, this.locationPattern);
//				ParentNode.GetNameSpace().SetSymbol(NameSymbol, (BStringNode)ValueNode);
				return new EmptyNode(ParentNode, null);
			}
		}
		return null;
	}
}
