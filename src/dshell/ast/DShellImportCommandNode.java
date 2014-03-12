package dshell.ast;

import java.util.ArrayList;

import dshell.grammar.CommandSymbolPattern;
import dshell.lang.DShellGrammar;
import dshell.lib.Utils;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.ast.ZTopLevelNode;
import zen.parser.ZLogger;
import zen.parser.ZNameSpace;
import zen.parser.ZSyntax;
import zen.parser.ZToken;
import zen.util.LibZen;

public class DShellImportCommandNode extends ZTopLevelNode {
	private ArrayList<ZToken> CommandTokenList;

	public DShellImportCommandNode(ZNode ParentNode) {
		super(ParentNode, null, 0);
		this.CommandTokenList = new ArrayList<ZToken>();
	}

	public void AppendTokenList(ArrayList<ZToken> TokenList) {
		if(TokenList.isEmpty()) {
			return;
		}
		int StartIndex = TokenList.get(0).StartIndex;
		int EndIndex = TokenList.get(TokenList.size() - 1).EndIndex;
		ZToken CommandToken = new ZToken(TokenList.get(0).Source, StartIndex, EndIndex);
		this.CommandTokenList.add(CommandToken);
		TokenList.clear();
	}

	@Override
	public void Perform(ZNameSpace NameSpace) {	// do nothing
	}

	public void SetCommands() {
		for(ZToken CommandToken : this.CommandTokenList) {
			this.SetCommandSymbol(CommandToken);
		}
	}

	private void SetCommandSymbol(ZToken CommandToken) {	//TODO: absolute path
		String CommandPath = CommandToken.GetText();
		if(CommandPath.equals("~")) {
			CommandPath = Utils.getEnv("HOME");
		}
		else if(CommandPath.startsWith("~/")) {
			CommandPath = Utils.getEnv("HOME") + CommandPath.substring(1);
		}
		
		ZNameSpace NameSpace = this.ParentNode.GetNameSpace();
		int loc = CommandPath.lastIndexOf('/');
		String Command = CommandPath;
		if(loc != -1) {
			if(!Utils.isFileExecutable(CommandPath)) {
				ZLogger._LogWarning(CommandToken, "unknown command");
				return;
			}
			Command = CommandPath.substring(loc + 1);
		}
		else {
			if(!Utils.isUnixCommand(CommandPath)) {
				ZLogger._LogWarning(CommandToken, "unknown command");
				return;
			}
		}
		ZSyntax Syntax = NameSpace.GetSyntaxPattern(Command);
		if(Syntax != null && !(Syntax.MatchFunc instanceof CommandSymbolPattern)) {
			if(LibZen.DebugMode) {
				System.err.println("found duplicated syntax pattern: " + Syntax);
			}
			return;
		}
		NameSpace.SetLocalSymbol(DShellGrammar.toCommandSymbol(Command), new ZStringNode(this.ParentNode, null, CommandPath));
	}
}
