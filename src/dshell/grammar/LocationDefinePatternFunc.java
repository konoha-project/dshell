package dshell.grammar;

import libbun.ast.BNode;
import libbun.ast.EmptyNode;
import libbun.parser.classic.BTokenContext;
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
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		tokenContext.MoveNext();
		BNode node = tokenContext.ParsePattern(parentNode, "$Name$", BTokenContext._Required);
		if(!node.IsErrorNode() && tokenContext.MatchToken("=")) {
			BNode valueNode = tokenContext.ParsePattern(parentNode, "$StringLiteral$", BTokenContext._Required);
			if(!valueNode.IsErrorNode()) {
//				String NameSymbol = ((BGetNameNode)Node).GetName();
//				ParentNode.GetNameSpace().DefineExpression(NameSymbol, this.locationPattern);
//				ParentNode.GetNameSpace().SetSymbol(NameSymbol, (BStringNode)ValueNode);
				return new EmptyNode(parentNode);
			}
		}
		return null;
	}
}
