package dshell.grammar;

import java.util.ArrayList;

import dshell.ast.DShellImportCommandNode;
import zen.ast.ZNode;
import zen.util.ZMatchFunction;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class ImportCommandPattern extends ZMatchFunction {
	public final static String PatternName = "$ImportCommand$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		DShellImportCommandNode Node = new DShellImportCommandNode(ParentNode);
		ArrayList<ZToken> TokenList = new ArrayList<ZToken>();
		TokenContext.MoveNext();
		while(TokenContext.HasNext()) {
			ZToken Token = TokenContext.GetToken();
			if(Token.EqualsText(";") || Token.IsIndent()) {
				break;
			}
			if(!Token.EqualsText(",")) {
				TokenList.add(Token);
			}
			if(Token.IsNextWhiteSpace()) {
				Node.AppendTokenList(TokenList);
			}
			TokenContext.MoveNext();
		}
		Node.AppendTokenList(TokenList);
		Node.SetCommands();
		return Node;
	}
}
