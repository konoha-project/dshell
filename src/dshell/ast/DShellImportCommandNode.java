package dshell.ast;

import java.util.ArrayList;

import dshell.grammar.CommandSymbolPattern;
import dshell.lang.DShellGrammar;
import dshell.lib.Utils;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.ast.ZTopLevelNode;
import zen.parser.ZNameSpace;
import zen.parser.ZSyntax;

public class DShellImportCommandNode extends ZTopLevelNode {
	private ArrayList<String> CommandPathList;

	public DShellImportCommandNode(ZNode ParentNode) {
		super(ParentNode, null, 0);
		this.CommandPathList = new ArrayList<String>();
	}

	public void AppendCommand(String CommandPath) {
		this.CommandPathList.add(CommandPath);
	}

	@Override
	public void Perform(ZNameSpace NameSpace) {
		for(String Path : this.CommandPathList) {
			this.SetCommandSymbol(this.ParentNode, Path);
		}
	}

	private void SetCommandSymbol(ZNode ParentNode, String CommandPath) {
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
		NameSpace.SetLocalSymbol(DShellGrammar.toCommandSymbol(Command), new ZStringNode(ParentNode, null, CommandPath));
	}
}
