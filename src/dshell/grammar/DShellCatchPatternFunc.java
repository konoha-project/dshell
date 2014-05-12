package dshell.grammar;

import libbun.ast.BNode;
import libbun.parser.classic.BTokenContext;
import libbun.util.BMatchFunction;
import dshell.ast.DShellCatchNode;

public class DShellCatchPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BNode catchNode = new DShellCatchNode(parentNode);
		catchNode = tokenContext.MatchToken(catchNode, "catch", BTokenContext._Required);
		catchNode = tokenContext.MatchToken(catchNode, "(", BTokenContext._Required);
		catchNode = tokenContext.MatchPattern(catchNode, DShellCatchNode._NameInfo, "$Name$", BTokenContext._Required);
		catchNode = tokenContext.MatchPattern(catchNode, DShellCatchNode._TypeInfo, "$TypeAnnotation$", BTokenContext._Optional);
		catchNode = tokenContext.MatchToken(catchNode, ")", BTokenContext._Required);
		catchNode = tokenContext.MatchPattern(catchNode, DShellCatchNode._Block, "$Block$", BTokenContext._Required);
		return catchNode;
	}
}
