package dshell.grammar;

import libbun.ast.BNode;
import libbun.ast.error.ErrorNode;
import libbun.parser.classic.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.ast.DShellTryNode;

public class DShellTryPatternFunc extends BMatchFunction {
	public final static String CatchPatternName = "$Catch$";

	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode TryNode = new DShellTryNode(ParentNode);
		TryNode = TokenContext.MatchToken(TryNode, "try", BTokenContext._Required);
		TryNode = TokenContext.MatchPattern(TryNode, DShellTryNode._Try, "$Block$", BTokenContext._Required);
		boolean foundCatchBlock = false;
		while(true) {
			if(TokenContext.IsNewLineToken("catch")) {
				TryNode = TokenContext.MatchPattern(TryNode, BNode._AppendIndex, DShellTryPatternFunc.CatchPatternName, BTokenContext._Required);
				foundCatchBlock = true;
				continue;
			}
			if(TokenContext.MatchNewLineToken("finally")) {
				TryNode = TokenContext.MatchPattern(TryNode, DShellTryNode._Finally, "$Block$", BTokenContext._Required);
			}
			break;
		}
		if(!foundCatchBlock) {
			return new ErrorNode(TryNode, "not found catch block");
		}
		return TryNode;
	}
}
