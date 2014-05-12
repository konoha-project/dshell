package dshell.grammar;

import libbun.ast.BNode;
import libbun.ast.error.ErrorNode;
import libbun.parser.classic.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.ast.DShellTryNode;

public class DShellTryPatternFunc extends BMatchFunction {
	public final static String catchPatternName = "$Catch$";

	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BNode tryNode = new DShellTryNode(parentNode);
		tryNode = tokenContext.MatchToken(tryNode, "try", BTokenContext._Required);
		tryNode = tokenContext.MatchPattern(tryNode, DShellTryNode._Try, "$Block$", BTokenContext._Required);
		boolean foundCatchBlock = false;
		while(true) {
			if(tokenContext.IsNewLineToken("catch")) {
				tryNode = tokenContext.MatchPattern(tryNode, BNode._AppendIndex, DShellTryPatternFunc.catchPatternName, BTokenContext._Required);
				foundCatchBlock = true;
				continue;
			}
			if(tokenContext.MatchNewLineToken("finally")) {
				tryNode = tokenContext.MatchPattern(tryNode, DShellTryNode._Finally, "$Block$", BTokenContext._Required);
			}
			break;
		}
		if(!foundCatchBlock) {
			return new ErrorNode(tryNode, "not found catch block");
		}
		return tryNode;
	}
}
