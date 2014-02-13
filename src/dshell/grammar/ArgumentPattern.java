package dshell.grammar;

import dshell.lang.DShellGrammar;
import zen.ast.ZBlockNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZNameSpace;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class ArgumentPattern extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		if(DShellGrammar.MatchStopToken(TokenContext)) {
			return null;
		}
		ZToken Token = TokenContext.GetToken();
		boolean HasStringExpr = false;
		String Path = null;
		if(Token.toString().startsWith("\"")) {
			Path = Token.GetText();
			if(Path.indexOf("${") != -1 && Path.lastIndexOf("}") != -1) {
				HasStringExpr = true;
			}
			TokenContext.MoveNext();
		}
		if(Path == null) {
			boolean FoundOpen = false;
			Path = "";
			while(TokenContext.HasNext()) {
				Token = TokenContext.GetToken();
				String ParsedText = Token.GetText();
				if(Token.IsIndent() || (!FoundOpen && DShellGrammar.MatchStopToken(TokenContext))) {
					break;
				}
				TokenContext.MoveNext();
				if(Token.EqualsText("$")) {   // $HOME/hoge
					ZToken Token2 = TokenContext.GetToken();
					String ParsedText2 = Token2.GetText();
					if(Token2.IsNameSymbol()) {
						Path += "${" + ParsedText2 + "}";
						HasStringExpr = true;
						TokenContext.MoveNext();
						if(DShellGrammar.IsNextWhiteSpace(Token2)) {
							break;
						}
						continue;
					}
				}
				if(Token.EqualsText("{")) {
					HasStringExpr = true;
					FoundOpen = true;
				}
				if(Token.EqualsText("}")) {
					FoundOpen = false;
				}
				if(Token.EqualsText("~")) {
					ParsedText = System.getenv("HOME");
				}
				Path += ParsedText;
				if(!FoundOpen && DShellGrammar.IsNextWhiteSpace(Token)) {
					break;
				}
			}
		}
		if(!HasStringExpr) {
			return new ZStringNode(ParentNode, Token, Path);
		}
		Path = "\"" + Path + "\"";
		Path = Path.replaceAll("\\$\\{", "\" + ");
		Path = Path.replaceAll("\\}", " + \"");
		ZNameSpace NameSpace = ParentNode.GetNameSpace();
		new ZTokenContext(TokenContext.Generator, NameSpace, Token.GetFileName(), Token.GetLineNumber(), Path);
		ZTokenContext LocalContext = new ZTokenContext(TokenContext.Generator, NameSpace, Token.GetFileName(), Token.GetLineNumber(), Path);
		return LocalContext.ParsePattern(new ZBlockNode(NameSpace), "$Expression$", ZTokenContext.Required);
	}

}
