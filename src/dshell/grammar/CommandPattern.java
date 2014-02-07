package dshell.grammar;

import dshell.ast.DShellDummyNode;
import dshell.lang.DShellGrammar;
import dshell.util.Utils;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.deps.ZMatchFunction;
import zen.parser.ZNameSpace;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class CommandPattern extends ZMatchFunction {
	private DShellPattern dshellPattern;
	public CommandPattern() {
		this.dshellPattern = new DShellPattern();
	}

	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		String Command = "";
		boolean foundSlash = true;
		String KeySymbol = null;
		String ParsedText = null;
		TokenContext.MoveNext();
		while(TokenContext.HasNext()) {
			ZToken Token = TokenContext.GetToken();
			ParsedText = Token.GetText();
			if(foundSlash && !Token.EqualsText("/")) {
				foundSlash = false;
				KeySymbol = ParsedText;
			}
			if(Token.EqualsText(",")) {
				ParsedText = "";
			}
			if(Token.EqualsText("~")) {
				ParsedText = System.getenv("HOME");
			}
			if(Token.EqualsText("/")) {
				foundSlash = true;
			}
			if(Token.EqualsText(";") || Token.IsIndent()) {
				break;
			}
			Command += ParsedText;
			if(DShellGrammar.IsNextWhiteSpace(Token)) {
				AppendCommand(ParentNode, Command, KeySymbol);
				Command = "";
				foundSlash = true;
			}
			TokenContext.MoveNext();
		}
		if(!Command.equals("")) {
			AppendCommand(ParentNode, Command, KeySymbol);
		}
		return new DShellDummyNode(ParentNode);
	}

	private void AppendCommand(ZNode ParentNode, String CommandPath, String CommandPrefix) {
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
		NameSpace.DefineExpression(Command, this.dshellPattern);
		if(!CommandPrefix.equals(CommandPath)) {	//FIXME: check duplication
			NameSpace.DefineExpression(CommandPrefix, this.dshellPattern);
		}
		NameSpace.SetLocalSymbol(DShellGrammar.CommandSymbol(Command), new ZStringNode(ParentNode, null, CommandPath));
	}
}
