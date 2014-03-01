package dshell.grammar;

import dshell.ast.DShellDummyNode;
import dshell.lang.DShellGrammar;
import dshell.lib.Utils;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZNameSpace;
import zen.parser.ZSyntax;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class ImportCommandPattern extends ZMatchFunction {
	public final static String PatternName = "$ImportCommand$";
	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		String Command = "";
		String ParsedText = null;
		TokenContext.MoveNext();
		while(TokenContext.HasNext()) {
			ZToken Token = TokenContext.GetToken();
			ParsedText = Token.GetText();
			if(Token.EqualsText(",")) {
				ParsedText = "";
			}
			if(Token.EqualsText("~")) {
				ParsedText = System.getenv("HOME");
			}
			if(Token.EqualsText(";") || Token.IsIndent()) {
				break;
			}
			Command += ParsedText;
			if(Token.IsNextWhiteSpace()) {
				AppendCommand(ParentNode, Command);
				Command = "";
			}
			TokenContext.MoveNext();
		}
		if(!Command.equals("")) {
			AppendCommand(ParentNode, Command);
		}
		return new DShellDummyNode(ParentNode);
	}

	private void AppendCommand(ZNode ParentNode, String CommandPath) {
		if(CommandPath.length() == 0) {
			return;
		}
		ZNameSpace NameSpace = ParentNode.GetNameSpace();
		int loc = CommandPath.lastIndexOf('/');
		String Command = CommandPath;
		if(loc != -1) {
			if(!Utils.isFileExecutable(CommandPath)) {	//FIXME: error report
				//NameSpace.Generator.Logger.Report(ZLogger.ErrorLevel, SourceToken, "not executable: " + CommandPath);
				System.err.println("not executable: " + CommandPath);
				return;
			}
			Command = CommandPath.substring(loc + 1);
		}
		else {
			if(!Utils.isUnixCommand(CommandPath)) {
				//NameSpace.Generator.Logger.Report(ZLogger.ErrorLevel, SourceToken, "unknown command: " + CommandPath);
				System.err.println("unknown command: " + CommandPath);
				return;
			}
		}
		ZSyntax Syntax = NameSpace.GetSyntaxPattern(Command);
		if(Syntax != null && !(Syntax.MatchFunc instanceof CommandSymbolPattern)) {
			//System.err.println("found duplicated syntax pattern: " + Syntax);
			return;
		}
		NameSpace.SetLocalSymbol(DShellGrammar.CommandSymbol(Command), new ZStringNode(ParentNode, null, CommandPath));
	}
}
