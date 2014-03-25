package dshell.grammar;

import java.util.ArrayList;

import dshell.lang.DShellGrammar;
import dshell.lib.Utils;
import zen.ast.ZEmptyNode;
import zen.ast.ZNode;
import zen.ast.ZStringNode;
import zen.util.LibZen;
import zen.util.ZMatchFunction;
import zen.parser.ZLogger;
import zen.parser.ZNameSpace;
import zen.parser.ZSyntax;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class ImportCommandPatternFunc extends ZMatchFunction {
	public final static String PatternName = "$ImportCommand$";

	public String ResolveHome(String Path) { //FIXME: 
		return Utils.resolveHome(Path);
	}

	public boolean isFileExecutable(String Path) { //FIXME
		return Utils.isFileExecutable(Path);
	}

	public boolean isUnixCommand(String cmd) { //FIXME
		return Utils.isUnixCommand(cmd);
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
				ZLogger._LogWarning(CommandToken, "unknown command");
				return;
			}
			Command = CommandPath.substring(loc + 1);
		}
		else {
			if(!this.isUnixCommand(CommandPath)) {
				ZLogger._LogWarning(CommandToken, "unknown command");
				return;
			}
		}
		ZSyntax Syntax = NameSpace.GetSyntaxPattern(Command);
		if(Syntax != null && !(Syntax.MatchFunc instanceof CommandSymbolPatternFunc)) {
			if(LibZen.DebugMode) {
				System.err.println("found duplicated syntax pattern: " + Syntax);
			}
			return;
		}
		NameSpace.SetLocalSymbol(DShellGrammar.toCommandSymbol(Command), new ZStringNode(ParentNode, null, CommandPath));
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
