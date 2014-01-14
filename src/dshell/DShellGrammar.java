package dshell;

import dshell.ast.DShellCommandNode;

import zen.ast.ZenEmptyNode;
import zen.ast.ZenErrorNode;
import zen.ast.ZenNode;
import zen.ast.ZenStringNode;
import zen.deps.LibNative;
import zen.deps.LibZen;
import zen.lang.ZenGrammar;
import zen.lang.ZenSystem;
import zen.parser.ZenNameSpace;
import zen.parser.ZenParserConst;
import zen.parser.ZenToken;
import zen.parser.ZenTokenContext;

public class DShellGrammar {
	private final static String FileOperators = "-d -e -f -r -w -x";
	private final static String StopTokens = ";,)]}&&||";

	private static String CommandSymbol(String Symbol) {
		return "__$" + Symbol;
	}

	private static void AppendCommand(ZenNameSpace NameSpace, String CommandPath, ZenToken SourceToken) {
		if(CommandPath.length() > 0) {
			/*local*/int loc = CommandPath.lastIndexOf('/');
			/*local*/String Command = CommandPath;
			if(loc != -1) {
				if(!Utils.isFileExecutable(CommandPath)) {
					//NameSpace.Context.ReportError(ErrorLevel, SourceToken, "not executable: " + CommandPath);
					System.err.println("not executable: " + CommandPath); //FIXME
				} else {
					Command = CommandPath.substring(loc+1);
					NameSpace.SetSymbol(Command, NameSpace.GetSyntaxPattern("$DShell$"), SourceToken);
					NameSpace.SetSymbol(DShellGrammar.CommandSymbol(Command), CommandPath, null);
				}
			} else {
				if(Utils.isUnixCommand(CommandPath)) {
					NameSpace.SetSymbol(Command, NameSpace.GetSyntaxPattern("$DShell$"), SourceToken);
					NameSpace.SetSymbol(DShellGrammar.CommandSymbol(Command), CommandPath, null);
				} else {
					//NameSpace.Context.ReportError(ErrorLevel, SourceToken, "unknown command: " + CommandPath);
					System.err.println("unknown command: " + CommandPath);
				}
			}
		}
	}

	public static long ShellCommentToken(ZenTokenContext TokenContext, String SourceText, long pos) {
		if(LibZen.CharAt(SourceText, pos) == '#') { // shell style SingleLineComment
			/*local*/long NextPos = pos + 1;
			while(NextPos < SourceText.length()) {
				/*local*/char NextChar = LibZen.CharAt(SourceText, NextPos);
				if(NextChar == '\n') {
					break;
				}
				NextPos = NextPos + 1;
			}
			return ZenGrammar.IndentToken(TokenContext, SourceText, NextPos);
		}
		return ZenParserConst.MismatchedPosition;
	}

	public static ZenNode MatchEnv(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		TokenContext.GetTokenAndMoveForward();
		ZenToken Token = TokenContext.GetTokenAndMoveForward();
		if(!LibZen.IsVariableName(Token.ParsedText, 0)) {
			return new ZenErrorNode(Token, "name");
		}
		String Name = Token.ParsedText;
		String Env  = System.getenv(Name);
		if(TokenContext.MatchToken("=")) {
			if(Env == null) {
				ZenNode ConstNode = TokenContext.ParsePattern(NameSpace, "$Expression$", ZenParserConst.Required);
				if(ConstNode.IsErrorNode()) {
					return ConstNode;
				}
				Env = ((ZenStringNode)ConstNode).Value;
			}
		}
		if(Env == null) {
			return new ZenErrorNode(Token, "undefined environment variable: " + Name);
		}
		else {
			NameSpace.SetSymbol(Name, Env, Token);
		}
		return new ZenEmptyNode(Token);
	}

	public static ZenNode MatchCommand(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		/*local*/String Command = "";
		/*local*/ZenToken SourceToken = null;
		TokenContext.GetTokenAndMoveForward();
		while(TokenContext.HasNext()) {
			/*local*/ZenToken Token = TokenContext.GetTokenAndMoveForward();
			if(Token.EqualsText(",")) {
				Token.ParsedText = "";
			}
			if(Token.EqualsText("~")) {
				Token.ParsedText = System.getenv("HOME");
			}
			if(Token.IsDelim() || Token.IsIndent()) {
				break;
			}
			SourceToken = Token;
			Command += Token.ParsedText;
			if(Token.IsNextWhiteSpace()) {
				AppendCommand(NameSpace, Command, SourceToken);
				Command = "";
				if(SourceToken.IsError()) {
					return new ZenErrorNode(SourceToken, "");
				}
			}
		}
		AppendCommand(NameSpace, Command, SourceToken);
		if(SourceToken.IsError()) {
			return new ZenErrorNode(SourceToken, "");
		}
		return new ZenEmptyNode(SourceToken); //FIXME
	}

	public static ZenNode MatchFileOperator(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		return null;
	}

	public static ZenNode MatchFilePath(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		/*local*/ZenToken Token = TokenContext.GetToken();
		/*local*/boolean HasStringExpr = false;
		/*local*/String Path = null;
		if(Token.IsIndent() || DShellGrammar.StopTokens.indexOf(Token.ParsedText) != -1) {
			return null;
		}
		else if(Token.IsQuoted()) {
			Path = LibZen.UnquoteString(Token.ParsedText);
			if(Path.indexOf("${") != -1) {
				HasStringExpr = true;
			}
			TokenContext.GetTokenAndMoveForward();
		}
		if(Path == null) {
			/*local*/boolean FoundOpen = false;
			Path = "";
			while(TokenContext.HasNext()) {
				Token = TokenContext.GetToken();
				/*local*/String ParsedText = Token.ParsedText;
				if(Token.IsIndent() || (!FoundOpen && DShellGrammar.StopTokens.indexOf(Token.ParsedText) != -1)) {
					break;
				}
				TokenContext.GetTokenAndMoveForward();
				if(Token.EqualsText("$")) {   // $HOME/hoge
					/*local*/ZenToken Token2 = TokenContext.GetToken();
					if(LibZen.IsVariableName(Token2.ParsedText, 0)) {
						Path += "${" + Token2.ParsedText + "}";
						HasStringExpr = true;
						TokenContext.GetTokenAndMoveForward();
						if(Token2.IsNextWhiteSpace()) {
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
				if(!FoundOpen && Token.IsNextWhiteSpace()) {
					break;
				}
			}
		}
		if(!HasStringExpr) {
			return new ZenStringNode(Token, Path);
		}
		Path = "\"" + Path + "\"";
		Path = Path.replaceAll("\\$\\{", "\" + (");
		Path = Path.replaceAll("\\}", ") + \"");
		ZenTokenContext LocalContext = new ZenTokenContext(NameSpace, Path, Token.FileLine);
		return LocalContext.ParsePattern(NameSpace, "$Expression$", ZenParserConst.Required);
	}

	public static ZenNode MatchDShell(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		ZenToken CommandToken = TokenContext.GetToken();
		ZenNode CommandNode = null;
		String Command = (String)NameSpace.GetSymbol(DShellGrammar.CommandSymbol(CommandToken.ParsedText));
		
		if(Command != null) {
			CommandNode = new DShellCommandNode(ZenSystem.VoidType, new ZenStringNode(CommandToken, Command));
			TokenContext.GetTokenAndMoveForward();
		}
		else { //FIXME
			//CommandTree.AppendMatchedPattern(NameSpace, TokenContext, "$FilePath$", Required);
			return new ZenErrorNode(CommandToken, "undefined command symbol");
		}
		TokenContext.SetBackTrack(false);
		while(TokenContext.HasNext()) {
			ZenToken Token = TokenContext.GetToken();
			if(Token.IsIndent() || StopTokens.indexOf(Token.ParsedText) != -1) {
				if(!Token.EqualsText("|") && !Token.EqualsText("&")) {
					break;
				}
			}
			if(Token.EqualsText("||") || Token.EqualsText("&&")) {
				return new ZenErrorNode(Token, "not implemented"); //FIXME
			}
			if(Token.EqualsText("|")) {
				TokenContext.GetTokenAndMoveForward();
				ZenNode pipedNode = TokenContext.ParsePattern(NameSpace, "$DShell$", ZenParserConst.Required);
				if(pipedNode.IsErrorNode()) {
					return pipedNode;
				}
				((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)pipedNode);
				return CommandNode;
			}
			CommandNode = TokenContext.AppendMatchedPattern(CommandNode, NameSpace, "$FilePath$", ZenParserConst.Required);
		}
		return CommandNode;
	}

	public static void ImportGrammar(ZenNameSpace NameSpace, Class<?> Grammar) {
		NameSpace.AppendTokenFunc("#", LibNative.LoadTokenFunc(Grammar, "ShellCommentToken")); 
		
		NameSpace.AppendSyntax("letenv", LibNative.LoadMatchFunc(Grammar, "MatchEnv"));
		NameSpace.AppendSyntax("command", LibNative.LoadMatchFunc(Grammar, "MatchCommand"));
		NameSpace.AppendSyntax("-", LibNative.LoadMatchFunc(Grammar, "MatchFileOperator"));
		NameSpace.AppendSyntax("$FilePath$", LibNative.LoadMatchFunc(Grammar, "MatchFilePath"));
		NameSpace.AppendSyntax("$DShell$", LibNative.LoadMatchFunc(Grammar, "MatchDShell"));
		NameSpace.Generator.SetGrammarInfo("dshell0.1");
	}
}
