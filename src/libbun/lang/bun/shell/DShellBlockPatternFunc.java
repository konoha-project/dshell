package libbun.lang.bun.shell;

import libbun.ast.BunBlockNode;
import libbun.ast.BNode;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.parser.LibBunGamma;
import libbun.util.BMatchFunction;

public class DShellBlockPatternFunc extends BMatchFunction {
	public final static String PatternName = "$Block$";

	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode BlockNode = new BunBlockNode(ParentNode, null);
		LibBunGamma CurrentNameSpace = TokenContext.Gamma;
		TokenContext.Gamma = ((BunBlockNode)BlockNode).GetBlockGamma();
		BToken SkipToken = TokenContext.GetToken();
		BlockNode = TokenContext.MatchToken(BlockNode, "{", BTokenContext._Required);
		if(!BlockNode.IsErrorNode()) {
			boolean Remembered = TokenContext.SetParseFlag(BTokenContext._AllowSkipIndent); // init
			while(TokenContext.HasNext()) {
				if(TokenContext.MatchToken("}")) {
					break;
				}
				BlockNode = TokenContext.MatchPattern(BlockNode, BNode._AppendIndex, "$Statement$", BTokenContext._Required);
				if(BlockNode.IsErrorNode()) {
					TokenContext.SkipError(SkipToken);
					TokenContext.MatchToken("}");
					break;
				}
			}
			TokenContext.SetParseFlag(Remembered);
		}
		TokenContext.Gamma = CurrentNameSpace;
		return BlockNode;
	}
}
