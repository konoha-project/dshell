package dshell.grammar;

import java.util.ArrayList;

import dshell.lang.DShellGrammar;
import zen.ast.ZNode;
import zen.deps.LibZen;
import zen.deps.ZMatchFunction;
import zen.parser.ZNameSpace;
import zen.parser.ZPatternToken;
import zen.parser.ZSource;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class ArgumentPattern extends ZMatchFunction {
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		if(DShellGrammar.MatchStopToken(TokenContext)) {
			return null;
		}
		ArrayList<ZToken> TokenList = new ArrayList<ZToken>();
		ZToken Token;
		do {
			Token = TokenContext.GetToken();
			if(Token.EqualsText("$")) {
				Token = this.MatchStringInterpolation(TokenContext);
				if(Token == null) {
					return null;
				}
			}
			TokenList.add(Token);
			Token = TokenContext.GetToken(ZTokenContext.MoveNext);
			if(Token.IsNextWhiteSpace()) {
				break;
			}
		} while(!DShellGrammar.MatchStopToken(TokenContext));
		return this.CreateJoinedArgNode(ParentNode, TokenContext, TokenList);
	}

	private ZToken MatchStringInterpolation(ZTokenContext TokenContext) {
		ZToken KeyToken = TokenContext.GetToken(ZTokenContext.MoveNext);
		String Symbol = KeyToken.GetText();
		ZToken Token = TokenContext.GetToken();
		if(Token.EqualsText("{")) {
			while(TokenContext.HasNext()) {
				ZToken BodyToken = TokenContext.GetToken();
				if(BodyToken.EqualsText("}")) {
					Symbol += BodyToken.GetText();
					ZSource Source = new ZSource(KeyToken.GetFileName(), KeyToken.GetLineNumber(), Symbol, TokenContext);
					return new ZToken(Source, 0, Symbol.length());
				}
				Symbol += BodyToken.GetText();
				TokenContext.MoveNext();
			}
		}
		else if(Token.IsNameSymbol()) {
			Symbol += "{" + Token.GetText() + "}";
			ZSource Source = new ZSource(KeyToken.GetFileName(), KeyToken.GetLineNumber(), Symbol, TokenContext);
			return new ZToken(Source, 0, Symbol.length());
		}
		return null;
	}

	private ZNode CreateJoinedArgNode(ZNode ParentNode, ZTokenContext TokenContext, ArrayList<ZToken> TokenList) {
		String Symbol = "";
		for(ZToken Token : TokenList) {
			String TokenText = Token.GetText();
			if(Token instanceof ZPatternToken && ((ZPatternToken)Token).PresetPattern.EqualsName("$StringLiteral$")) {
				Symbol += this.ResolveStringInterpolation(LibZen._UnquoteString(TokenText));
			}
			else if(Token.EqualsText("~")) {
				Symbol += System.getenv("HOME");
			}
			else if(TokenText.startsWith("${")) {
				Symbol += "\" + " + TokenText.substring(2, TokenText.length() - 1) + " + \"";
			}
			else {
				Symbol += TokenText;
			}
		}
		String FileName = TokenList.get(0).GetFileName();
		int LineNum = TokenList.get(0).GetLineNumber();
		Symbol = "\"" + Symbol + "\"";
		ZNameSpace NameSpace = ParentNode.GetNameSpace();
		ZTokenContext LocalContext = new ZTokenContext(TokenContext.Generator, NameSpace, FileName, LineNum, Symbol);
		return LocalContext.ParsePattern(ParentNode, "$Statement$", ZTokenContext.Required);
	}

	private String ResolveStringInterpolation(String Value) {	//TODO;
		return Value;
	}
}
