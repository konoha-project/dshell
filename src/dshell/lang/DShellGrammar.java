package dshell.lang;

import dshell.ast.DShellCommandNode;
import dshell.util.Utils;

import zen.ast.ZenEmptyNode;
import zen.ast.ZenErrorNode;
import zen.ast.ZenIntNode;
import zen.ast.ZenNode;
import zen.ast.ZenStringNode;
import zen.deps.LibNative;
import zen.deps.LibZen;
import zen.lang.ZenGrammar;
import zen.lang.ZenSystem;
import zen.parser.ZenNameSpace;
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
			int loc = CommandPath.lastIndexOf('/');
			String Command = CommandPath;
			if(loc != -1) {
				if(!Utils.isFileExecutable(CommandPath)) {
					System.err.println("not executable: " + CommandPath); //FIXME: error report
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
					System.err.println("unknown command: " + CommandPath); //FIXME: error report
				}
			}
		}
	}

	public static long ShellCommentToken(ZenTokenContext TokenContext, String SourceText, long pos) {
		if(LibZen.CharAt(SourceText, pos) == '#') { // shell style SingleLineComment
			long NextPos = pos + 1;
			while(NextPos < SourceText.length()) {
				char NextChar = LibZen.CharAt(SourceText, NextPos);
				if(NextChar == '\n') {
					break;
				}
				NextPos++;
			}
			return ZenGrammar.IndentToken(TokenContext, SourceText, NextPos);
		}
		return ZenTokenContext.MismatchedPosition;
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
				ZenNode ConstNode = TokenContext.ParsePattern(NameSpace, "$Expression$", ZenTokenContext.Required);
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
		String Command = "";
		ZenToken SourceToken = null;
		TokenContext.GetTokenAndMoveForward();
		while(TokenContext.HasNext()) {
			ZenToken Token = TokenContext.GetTokenAndMoveForward();
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
		return new ZenEmptyNode(SourceToken);
	}

	public static ZenNode MatchFileOperator(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		return null;
	}

	public static ZenNode MatchFilePath(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		ZenToken Token = TokenContext.GetToken();
		boolean HasStringExpr = false;
		String Path = null;
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
			boolean FoundOpen = false;
			Path = "";
			while(TokenContext.HasNext()) {
				Token = TokenContext.GetToken();
				String ParsedText = Token.ParsedText;
				if(Token.IsIndent() || (!FoundOpen && DShellGrammar.StopTokens.indexOf(Token.ParsedText) != -1)) {
					break;
				}
				TokenContext.GetTokenAndMoveForward();
				if(Token.EqualsText("$")) {   // $HOME/hoge
					ZenToken Token2 = TokenContext.GetToken();
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
		return LocalContext.ParsePattern(NameSpace, "$Expression$", ZenTokenContext.Required);
	}

	public static DShellCommandNode SetOptionAndReturn(ZenNode Node, int optionFlag) {
		ZenIntNode OptionNode = new ZenIntNode(new ZenToken(0, Integer.toString(optionFlag), 0), optionFlag);
		DShellCommandNode CommandNode = (DShellCommandNode)Node;
		CommandNode.AppendOption(OptionNode);
		return CommandNode;
	}

	public static ZenNode MatchDShell(ZenNameSpace NameSpace, ZenTokenContext TokenContext, ZenNode LeftNode) {
		int optionFlag = Utils.printable;
		ZenToken CommandToken = TokenContext.GetToken();
		ZenNode CommandNode = null;
		String Command = (String)NameSpace.GetSymbol(DShellGrammar.CommandSymbol(CommandToken.ParsedText));

		if(Command != null) {
			CommandNode = new DShellCommandNode(ZenSystem.VoidType, new ZenStringNode(CommandToken, Command));
			TokenContext.GetTokenAndMoveForward();
		}
		else {
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
			if(Token.EqualsText("||") || Token.EqualsText("&&")) { //TODO: support context type
//				ZenSyntaxPattern SuffixPattern = TokenContext.GetSuffixPattern(NameSpace);
//				return TokenContext.ApplyMatchPattern(NameSpace, CommandNode, SuffixPattern);
			}
			if(Token.EqualsText("|")) {
				TokenContext.GetTokenAndMoveForward();
				ZenNode pipedNode = TokenContext.ParsePattern(NameSpace, "$DShell$", ZenTokenContext.Required);
				if(pipedNode.IsErrorNode()) {
					return pipedNode;
				}
				((DShellCommandNode)CommandNode).AppendPipedNextNode((DShellCommandNode)pipedNode);
				return SetOptionAndReturn(CommandNode, optionFlag);
			}
			if(Token.EqualsText("&")) {	// set background job
				TokenContext.GetTokenAndMoveForward();
				return SetOptionAndReturn(CommandNode, Utils.setFlag(optionFlag, Utils.background, true));
			}
			CommandNode = TokenContext.AppendMatchedPattern(CommandNode, NameSpace, "$FilePath$", ZenTokenContext.Required);
		}
		return SetOptionAndReturn(CommandNode, optionFlag);
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
