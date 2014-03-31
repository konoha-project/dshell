package dshell.grammar;

import java.util.ArrayList;

import dshell.lang.DShellGrammar;
import dshell.lib.Utils;
import libbun.parser.ast.ZEmptyNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZStringNode;
import libbun.util.LibZen;
import libbun.util.Var;
import libbun.util.ZMatchFunction;
import libbun.parser.ZNameSpace;
import libbun.parser.ZSyntax;
import libbun.parser.ZToken;
import libbun.parser.ZTokenContext;

public class ImportCommandPatternFunc extends ZMatchFunction {
	public final static String _PatternName = "$ImportCommand$";

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
		@Var int StartIndex = TokenList.get(0).StartIndex;
		@Var int EndIndex = TokenList.get(TokenList.size() - 1).EndIndex;
		@Var ZToken CommandToken = new ZToken(TokenList.get(0).Source, StartIndex, EndIndex);
		TokenList.clear();
		return CommandToken;
	}

	private void SetCommandSymbol(ZNode ParentNode, ArrayList<ZToken> TokenList) {
		@Var ZToken CommandToken = this.ToCommandToken(TokenList);
		if(CommandToken == null) {
			return;
		}
		@Var String CommandPath = this.ResolveHome(CommandToken.GetText());
		@Var ZNameSpace NameSpace = ParentNode.GetNameSpace();
		@Var int loc = CommandPath.lastIndexOf('/');
		@Var String Command = CommandPath;
		if(loc != -1) {
			if(!this.isFileExecutable(CommandPath)) {
				System.err.println("[warning] unknown command: " + CommandPath);
				return;
			}
			Command = CommandPath.substring(loc + 1);
		}
		else {
			@Var String FullPath = this.getUnixCommand(CommandPath);
			if(FullPath == null) {
				System.err.println("[warning] unknown command: " + CommandPath);
				return;
			}
			CommandPath = FullPath;
		}
		@Var ZSyntax Syntax = NameSpace.GetSyntaxPattern(Command);
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
		@Var ArrayList<ZToken> TokenList = new ArrayList<ZToken>();
		TokenContext.MoveNext();
		while(TokenContext.HasNext()) {
			@Var ZToken Token = TokenContext.GetToken();
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
