package dshell.grammar;

import java.util.ArrayList;

import dshell.lang.DShellGrammar;
import dshell.lib.Utils;
import libbun.parser.ast.ZEmptyNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.util.LibZen;
import libbun.util.ZMatchFunction;
import libbun.parser.ZNameSpace;
import libbun.parser.ZSyntax;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class ImportCommandPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$ImportCommand$";

	public String ResolveHome(String Path) { //FIXME: 
		return Utils.resolveHome(Path);
	}

	public boolean isFileExecutable(String Path) { //FIXME
		return Utils.isFileExecutable(Path);
	}

	public String getUnixCommand(String cmd) { //FIXME
		return Utils.getCommandFromPath(cmd);
	}

	private ZToken ToCommandToken(ArrayList<ZToken> TokenList) {
		if(TokenList.isEmpty()) {
			return null;
		}
		int StartIndex = TokenList.get(0).StartIndex;
		int EndIndex = TokenList.get(TokenList.size() - 1).EndIndex;
		ZToken CommandToken = new ZToken(TokenList.get(0).Source, StartIndex, EndIndex);
		TokenList.clear();
		return CommandToken;
	}

	private void SetCommandSymbol(ZNode ParentNode, ArrayList<ZToken> TokenList) {
		ZToken CommandToken = this.ToCommandToken(TokenList);
		if(CommandToken == null) {
			return;
		}
		String CommandPath = this.ResolveHome(CommandToken.GetText());
		ZNameSpace NameSpace = ParentNode.GetNameSpace();
		int loc = CommandPath.lastIndexOf('/');
		String Command = CommandPath;
		if(loc != -1) {
			if(!this.isFileExecutable(CommandPath)) {
				System.err.println("[warning] unknown command: " + CommandPath);
				return;
			}
			Command = CommandPath.substring(loc + 1);
		}
		else {
			String FullPath = this.getUnixCommand(CommandPath);
			if(FullPath == null) {
				System.err.println("[warning] unknown command: " + CommandPath);
				return;
			}
			CommandPath = FullPath;
		}
		ZSyntax Syntax = NameSpace.GetSyntaxPattern(Command);
		if(Syntax != null && !(Syntax.MatchFunc instanceof CommandSymbolPatternFunc)) {
			if(LibZen.DebugMode) {
				System.err.println("found duplicated syntax pattern: " + Syntax);
			}
			return;
		}
		NameSpace.SetSymbol(DShellGrammar.toCommandSymbol(Command), new ZStringNode(ParentNode, null, CommandPath));
	}

	@Override
	public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
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
				this.SetCommandSymbol(ParentNode, TokenList);
			}
			TokenContext.MoveNext();
		}
		this.SetCommandSymbol(ParentNode, TokenList);
		return new ZEmptyNode(ParentNode, null);
	}
}
