package dshell.grammar;

import java.util.ArrayList;
import java.util.TreeSet;

import dshell.ast.CommandNode;
import dshell.ast.sugar.DShellExportEnvNode;
import dshell.ast.sugar.DShellImportEnvNode;
import dshell.lib.BuiltinSymbol;
import dshell.lib.RuntimeContext;
import dshell.lib.Utils;
import dshell.ast.sugar.ArgumentNode;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.EmptyNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.classic.BPatternToken;
import libbun.parser.classic.BSourceContext;
import libbun.parser.classic.BToken;
import libbun.parser.classic.BTokenContext;
import libbun.parser.classic.LibBunGamma;
import libbun.parser.classic.LibBunSyntax;
import libbun.type.BType;
import libbun.util.BArray;
import libbun.util.BMatchFunction;
import libbun.util.BTokenFunction;
import libbun.util.LibBunSystem;

// Token
class ShellStyleCommentTokenFunc extends BTokenFunction {
	@Override
	public boolean Invoke(BSourceContext sourceContext) {
		while(sourceContext.HasChar()) {
			char ch = sourceContext.GetCurrentChar();
			if(ch == '\n') {
				break;
			}
			sourceContext.MoveNext();
		}
		return true;
	}
}

class CommandTokenFunc extends BTokenFunction {
	@Override
	public boolean Invoke(BSourceContext sourceContext) {
		int startIndex = sourceContext.GetPosition();
		StringBuilder symbolBuilder = new StringBuilder();
		for(int i = 0; sourceContext.HasChar(); i++) {
			char ch = sourceContext.GetCurrentChar();
			if(i != 0 && !this.matchAcceptableChar(ch)) {
				break;
			}
			symbolBuilder.append(ch);
			sourceContext.MoveNext();
		}
		String commandSymbol = symbolBuilder.toString();
		if(RuntimeContext.getContext().commandScope.isCommand(commandSymbol)) {
			sourceContext.Tokenize(CommandPatternFunc.patternName, startIndex, sourceContext.GetPosition());
			return true;
		}
		else if(commandSymbol.startsWith("//")) {
			return false;
		}
		else if(commandSymbol.equals("/") && sourceContext.HasChar() && sourceContext.GetCurrentChar() == '*') {
			return false;
		}
		else if(this.isValidFilePath(commandSymbol)) {
			sourceContext.Tokenize(CommandPatternFunc.patternName, startIndex, sourceContext.GetPosition());
			return true;
		}
		return false;
	}

	private boolean matchAcceptableChar(char ch) {
		if(Character.isLetterOrDigit(ch)) {
			return true;
		}
		switch(ch) {
		case '-':
		case '+':
		case '_':
		case '/':
		case '.':
			return true;
		default:
			return false;
		}
	}

	private boolean isValidFilePath(String commandPath) {
		int fileSeparaterIndex = commandPath.lastIndexOf("/");
		if(fileSeparaterIndex != -1 && fileSeparaterIndex != commandPath.length() - 1) {
			return true;
		}
		return false;
	}
}

// Syntax Pattern
class ImportPatternFunc extends BMatchFunction {
	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		tokenContext.MoveNext();
		BToken token = tokenContext.GetToken();
		if(token.EqualsText("command")) {
			BNode node = tokenContext.ParsePattern(parentNode, ImportCommandAsPatternFunc.patternName, BTokenContext._Optional);
			if(node != null) {
				return node;
			}
			return tokenContext.ParsePattern(parentNode, ImportCommandPatternFunc.patternName, BTokenContext._Required);
		}
		if(token.EqualsText("env")) {
			return tokenContext.ParsePattern(parentNode, ImportEnvPatternFunc.patternName, BTokenContext._Required);
		}
		return null;
	}
}

class ImportCommandPatternFunc extends BMatchFunction {
	public final static String patternName = "$ImportCommand$";

	private void setCommandSymbol(BNode parentNode, ArrayList<BToken> tokenList) {
		String commandPath = ShellGrammar.resolveCommandPath(tokenList);
		int index = commandPath.lastIndexOf("/");
		ShellGrammar.checkDuplicationAndSetCommand(parentNode.GetGamma(), commandPath.substring(index + 1), commandPath);
	}

	private void importAllFromPath(BNode parentNode) {
		LibBunGamma gamma = parentNode.GetGamma();
		TreeSet<String> commandSet = Utils.getCommandSetFromPath(true);
		for(String commandPath : commandSet) {
			int lastIndex = commandPath.lastIndexOf("/");
			ShellGrammar.checkDuplicationAndSetCommand(gamma, commandPath.substring(lastIndex + 1), commandPath);
		}
	}

	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		ArrayList<BToken> tokenList = new ArrayList<BToken>();
		tokenContext.MoveNext();
		if(tokenContext.MatchToken("*")) {
			this.importAllFromPath(parentNode);
			return new EmptyNode(parentNode);
		}
		while(tokenContext.HasNext()) {
			BToken token = tokenContext.GetToken();
			if(token.EqualsText(";") || token.IsIndent()) {
				break;
			}
			if(!token.EqualsText(",")) {
				tokenList.add(token);
			}
			if(token.IsNextWhiteSpace()) {
				this.setCommandSymbol(parentNode, tokenList);
			}
			tokenContext.MoveNext();
		}
		this.setCommandSymbol(parentNode, tokenList);
		return new EmptyNode(parentNode);
	}
}

class ImportCommandAsPatternFunc extends BMatchFunction {
	public final static String patternName = "$ImportCommandAs$";

	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		ArrayList<BToken> tokenList = new ArrayList<BToken>();
		tokenContext.MoveNext();
		// match command path
		while(tokenContext.HasNext()) {
			BToken token = tokenContext.GetToken();
			if(token.EqualsText(",")) {
				return null;
			}
			tokenList.add(token);
			tokenContext.MoveNext();
			if(token.IsNextWhiteSpace()) {
				break;
			}
		}
		String commandName = ShellGrammar.toCommandToken(tokenList).GetText();
		if(!tokenContext.MatchToken("as")) {
			return null;
		}
		// match command name
		while(tokenContext.HasNext()) {
			BToken token = tokenContext.GetToken();
			if(token.EqualsText(";") || token.IsIndent()) {
				break;
			}
			tokenList.add(token);
			tokenContext.MoveNext();
		}
		String commandPath = ShellGrammar.resolveCommandPath(tokenList);
		ShellGrammar.checkDuplicationAndSetCommand(parentNode.GetGamma(), commandName, commandPath);
		return new EmptyNode(parentNode);
	}
}

class CommandPatternFunc extends BMatchFunction {
	public final static String patternName = "$CommandSymbol$";

	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		String command = this.checkAndGetCommandPath(tokenContext);
		if(command == null) {
			return null;
		}
		// Match Prefix Option
		if(command.equals(ShellGrammar.timeout) || command.equals(ShellGrammar.trace)) {
			return tokenContext.ParsePatternAfter(parentNode, leftNode, PrefixOptionPatternFunc.patternName, BTokenContext._Required);
		}
		CommandNode commandNode = new CommandNode(parentNode, tokenContext.GetToken(BTokenContext._MoveNext), command);
		while(tokenContext.HasNext()) {
			if(tokenContext.MatchToken("|")) {
				// Match Command Symbol
				BNode pipedNode = tokenContext.ParsePatternAfter(parentNode, commandNode, CommandPatternFunc.patternName, BTokenContext._Required);
				if(pipedNode.IsErrorNode()) {
					return pipedNode;
				}
				return commandNode.appendPipedNextNode((CommandNode)pipedNode);
			}
			// Match Redirect
			BNode redirectNode = tokenContext.ParsePattern(parentNode, RedirectPatternFunc.patternName, BTokenContext._Optional);
			if(redirectNode != null) {
				commandNode.appendPipedNextNode((CommandNode)redirectNode);
				continue;
			}
			// Match Suffix Option
			BNode suffixOptionNode = tokenContext.ParsePattern(parentNode, SuffixOptionPatternFunc._PatternName, BTokenContext._Optional);
			if(suffixOptionNode != null) {
				if(suffixOptionNode.IsErrorNode()) {
					return suffixOptionNode;
				}
				return commandNode.appendPipedNextNode((CommandNode)suffixOptionNode);
			}
			// Match Argument
			BNode argNode = tokenContext.ParsePattern(parentNode, CommandArgPatternFunc.patternName, BTokenContext._Optional);
			if(argNode == null) {
				break;
			}
			commandNode.appendArgNode(argNode);
		}
		return commandNode;
	}

	private String checkAndGetCommandPath(BTokenContext tokenContext) {
		if(ShellGrammar.matchStopToken(tokenContext)) {
			return null;
		}
		BToken commandToken = tokenContext.GetToken();
		if(!ShellGrammar.matchPatternToken(commandToken, CommandPatternFunc.patternName)) {
			return null;
		}
		String command = RuntimeContext.getContext().commandScope.getCommandPath(commandToken.GetText());
		command = command != null ? command : commandToken.GetText();
		return command;
	}
}

class CommandArgPatternFunc extends BMatchFunction {
	public final static String patternName = "$CommandArg$";

	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		if(ShellGrammar.matchStopToken(tokenContext)) {
			return null;
		}
		boolean foundSubstitution = false;
		boolean foundEscape = false;
		BArray<BToken> tokenList = new BArray<BToken>(new BToken[]{});
		BArray<BNode> nodeList = new BArray<BNode>(new BNode[]{});
		while(!ShellGrammar.matchStopToken(tokenContext)) {
			BToken token = tokenContext.GetToken(BTokenContext._MoveNext);
			if(ShellGrammar.matchPatternToken(token, DoubleQuoteStringLiteralPatternFunc.patternName)) {
				this.flush(tokenContext, nodeList, tokenList);
				BNode node = DoubleQuoteStringLiteralPatternFunc.interpolate(parentNode, tokenContext, token);
				if(node == null) {
					node = new BunStringNode(parentNode, null, LibBunSystem._UnquoteString(token.GetText()));
				}
				nodeList.add(node);
			}
			else if(ShellGrammar.matchPatternToken(token, SingleQuoteStringLiteralPatternFunc.patternName)) {
				this.flush(tokenContext, nodeList, tokenList);
				nodeList.add(new BunStringNode(parentNode, null, LibBunSystem._UnquoteString(token.GetText())));
			}
			else if(!foundEscape && token.EqualsText("$") && !token.IsNextWhiteSpace() && tokenContext.MatchToken("{")) {
				this.flush(tokenContext, nodeList, tokenList);
				BNode node = tokenContext.ParsePattern(parentNode, "$Expression$", BTokenContext._Required);
				node = tokenContext.MatchToken(node, "}", BTokenContext._Required);
				if(node.IsErrorNode()) {
					return node;
				}
				token = tokenContext.LatestToken;
				nodeList.add(node);
			}
			else if(!foundEscape && token.EqualsText("$") && !token.IsNextWhiteSpace() && tokenContext.GetToken().IsNameSymbol()) {
				this.flush(tokenContext, nodeList, tokenList);
				token = tokenContext.GetToken();
				BNode node = tokenContext.ParsePattern(parentNode, "$SymbolExpression$", BTokenContext._Required);
				if(node.IsErrorNode()) {
					return node;
				}
				nodeList.add(node);
			}
//			else if(!FoundEscape && Token.EqualsText("`")) {	//TODO
//				
//			}
			else if(!foundEscape && token.EqualsText("$") && !token.IsNextWhiteSpace() && tokenContext.MatchToken("(")) {
				this.flush(tokenContext, nodeList, tokenList);
				BNode node = tokenContext.ParsePattern(parentNode, PrefixOptionPatternFunc.patternName, BTokenContext._Optional);
				if(node == null) {
					node = tokenContext.ParsePattern(parentNode, CommandPatternFunc.patternName, BTokenContext._Required);
				}
				node = tokenContext.MatchToken(node, ")", BTokenContext._Required);
				if(node instanceof CommandNode) {
					((CommandNode)node).setType(BType.StringType);
				}
				token = tokenContext.LatestToken;
				nodeList.add(node);
				foundSubstitution = true;
			}
			else {
				tokenList.add(token);
			}
			if(token.IsNextWhiteSpace()) {
				break;
			}
			foundEscape = this.checkEscape(token, foundEscape);
		}
		this.flush(tokenContext, nodeList, tokenList);
		BNode argNode = new ArgumentNode(parentNode, foundSubstitution ? ArgumentNode._Substitution : ArgumentNode._Normal);
		argNode.SetNode(ArgumentNode._Expr, ShellGrammar.toNode(parentNode, tokenContext, nodeList));
		return argNode;
	}

	private boolean checkEscape(BToken token, boolean foundEscape) {
		if(token.EqualsText("\\") && !foundEscape) {
			return true;
		}
		return false;
	}

	private void flush(BTokenContext tokenContext, BArray<BNode> nodeList, BArray<BToken> tokenList) {
		int size = tokenList.size();
		if(size == 0) {
			return;
		}
		int startIndex = 0;
		int endIndex = 0;
		for(int i = 0; i < size; i++) {
			if(i == 0) {
				startIndex = BArray.GetIndex(tokenList, i).StartIndex;
			}
			if(i == size - 1) {
				endIndex = BArray.GetIndex(tokenList, i).EndIndex;
			}
		}
		BToken token = new BToken(tokenContext.SourceContext.Source, startIndex, endIndex);
		nodeList.add(new BunStringNode(null, token, LibBunSystem._UnquoteString(Utils.resolveHome(token.GetText()))));
		tokenList.clear(0);
	}
}

class RedirectPatternFunc extends BMatchFunction {
	public final static String patternName = "$Redirect$";

	// <, >, >>, >&, 1>, 2>, 1>>, 2>>, &>, &>>
	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BToken token = tokenContext.GetToken(BTokenContext._MoveNext);
		String redirectSymbol = token.GetText();
		if(token.EqualsText(">>") || token.EqualsText("<")) {
			return this.createRedirectNode(parentNode, tokenContext, redirectSymbol, true);
		}
		else if(token.EqualsText("&")) {
			BToken token2 = tokenContext.GetToken(BTokenContext._MoveNext);
			if(token2.EqualsText(">") || token2.EqualsText(">>")) {
				redirectSymbol += token2.GetText();
				return this.createRedirectNode(parentNode, tokenContext, redirectSymbol, true);
			}
		}
		else if(token.EqualsText(">")) {
			BToken token2 = tokenContext.GetToken();
			if(token2.EqualsText("&")) {
				redirectSymbol += token2.GetText();
				return this.createRedirectNode(parentNode, tokenContext, redirectSymbol, true);
			}
			return this.createRedirectNode(parentNode, tokenContext, redirectSymbol, true);
		}
		else if(token.EqualsText("1") || token.EqualsText("2")) {
			BToken token2 = tokenContext.GetToken(BTokenContext._MoveNext);
			if(token2.EqualsText(">>")) {
				redirectSymbol += token2.GetText();
				return this.createRedirectNode(parentNode, tokenContext, redirectSymbol, true);
			}
			else if(token2.EqualsText(">")) {
				redirectSymbol += token2.GetText();
				if(redirectSymbol.equals("2>") && tokenContext.MatchToken("&")) {
					if(tokenContext.MatchToken("1")) {
						return this.createRedirectNode(parentNode, tokenContext, "2>&1", false);
					}
					return null;
				}
				return this.createRedirectNode(parentNode, tokenContext, redirectSymbol, true);
			}
		}
		return null;
	}

	private BNode createRedirectNode(BNode parentNode, BTokenContext tokenContext, String redirectSymbol, boolean existTarget) {
		CommandNode node = new CommandNode(parentNode, null, redirectSymbol);
		if(existTarget) {
			BNode targetNode = tokenContext.ParsePattern(node, CommandArgPatternFunc.patternName, BTokenContext._Required);
			if(targetNode.IsErrorNode()) {
				return targetNode;
			}
			node.appendArgNode(targetNode);
		}
		return node;
	}
}

class PrefixOptionPatternFunc extends BMatchFunction {
	public final static String patternName = "$PrefixOption$";

	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BToken token = tokenContext.GetToken(BTokenContext._MoveNext);
		String symbol = token.GetText();
		if(symbol.equals(ShellGrammar.trace)) {
			BNode commandNode = tokenContext.ParsePattern(parentNode, CommandPatternFunc.patternName, BTokenContext._Required);
			if(commandNode.IsErrorNode()) {
				return commandNode;
			}
			CommandNode node = new CommandNode(parentNode, token, symbol);
			return node.appendPipedNextNode((CommandNode) commandNode);
		}
		if(symbol.equals(ShellGrammar.timeout) && leftNode == null) {
			BNode timeNode = this.parseTimeout(parentNode, tokenContext);
			if(timeNode.IsErrorNode()) {
				return timeNode;
			}
			BNode commandNode = tokenContext.ParsePattern(parentNode, CommandPatternFunc.patternName, BTokenContext._Required);
			if(commandNode.IsErrorNode()) {
				return commandNode;
			}
			CommandNode node = new CommandNode(parentNode, token, symbol);
			node.appendArgNode(timeNode);
			return node.appendPipedNextNode((CommandNode) commandNode);
		}
		return null;
	}

	public BNode parseTimeout(BNode parentNode, BTokenContext tokenContext) {
		BToken numToken = tokenContext.GetToken(BTokenContext._MoveNext);
		if((numToken instanceof BPatternToken)) {
			if(((BPatternToken)numToken).PresetPattern.PatternName.equals(("$IntegerLiteral$"))) {
				long num = LibBunSystem._ParseInt(numToken.GetText());
				if(num > 0) {
					if(numToken.IsNextWhiteSpace()) {
						return new ArgumentNode(parentNode, Long.toString(num));
					}
					BToken unitToken = tokenContext.GetToken(BTokenContext._MoveNext);
					String unitSymbol = unitToken.GetText();
					if(unitSymbol.equals("ms")) {
						return new ArgumentNode(parentNode, Long.toString(num));
					}
					if(unitSymbol.equals("s")) {
						return new ArgumentNode(parentNode, Long.toString(num * 1000));
					}
					if(unitSymbol.equals("m")) {
						return new ArgumentNode(parentNode, Long.toString(num * 1000 * 60));
					}
					return tokenContext.CreateExpectedErrorNode(unitToken, "{ms, s, m}");
				}
			}
		}
		return tokenContext.CreateExpectedErrorNode(numToken, "Integer Number Symbol");
	}
}

class SuffixOptionPatternFunc extends BMatchFunction {
	public final static String _PatternName = "$SuffixOption$";

	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BToken token = tokenContext.GetToken();
		tokenContext.MoveNext();
		String optionSymbol = token.GetText();
		if(token.EqualsText(ShellGrammar.background)) {	// set background job
			return new CommandNode(parentNode, token, optionSymbol);
		}
		return null;
	}
}

class ImportEnvPatternFunc extends BMatchFunction {
	public final static String patternName = "$ImportEnv$";

	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BNode node = new DShellImportEnvNode(parentNode);
		node = tokenContext.MatchToken(node, "env", BTokenContext._Required);
		node = tokenContext.MatchPattern(node, DShellImportEnvNode._NameInfo, "$Name$", BTokenContext._Required);
		return node;
	}
}

class ExportEnvPatternFunc extends BMatchFunction {
	public final static String patternName = "export";

	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		tokenContext.MoveNext();
		BNode node = new DShellExportEnvNode(parentNode);
		node = tokenContext.MatchToken(node, "env", BTokenContext._Required);
		node = tokenContext.MatchPattern(node, DShellExportEnvNode._NameInfo, "$Name$", BTokenContext._Required);
		node = tokenContext.MatchToken(node, "=", BTokenContext._Required);
		node = tokenContext.MatchPattern(node, DShellExportEnvNode._Expr, "$Expression$", BTokenContext._Required);
		return node;
	}
}

class DShellBlockPatternFunc extends BMatchFunction {
	public final static String patternName = "$Block$";

	@Override
	public BNode Invoke(BNode parentNode, BTokenContext tokenContext, BNode leftNode) {
		BNode blockNode = new BunBlockNode(parentNode, null);
		RuntimeContext.getContext().commandScope.createNewScope();
		blockNode = tokenContext.MatchToken(blockNode, "{", BTokenContext._Required);
		if(!blockNode.IsErrorNode()) {
			boolean remembered = tokenContext.SetParseFlag(BTokenContext._AllowSkipIndent); // init
			while(tokenContext.HasNext()) {
				if(tokenContext.MatchToken("}")) {
					break;
				}
				blockNode = tokenContext.MatchPattern(blockNode, BNode._AppendIndex, "$Statement$", BTokenContext._Required);
				if(blockNode.IsErrorNode()) {
					tokenContext.MatchToken("}");
					break;
				}
			}
			tokenContext.SetParseFlag(remembered);
		}
		RuntimeContext.getContext().commandScope.removeCurrentScope();
		return blockNode;
	}
}

public class ShellGrammar {
	// suffix option symbol
	public final static String background = "&";
	// prefix option symbol
	public final static String timeout = "timeout";
	public final static String trace = "trace";

	public static boolean matchStopToken(BTokenContext tokenContext) { // ;,)]}&&||
		BToken token = tokenContext.GetToken();
		if(!tokenContext.HasNext()) {
			return true;
		}
		if(token.IsIndent() || token.EqualsText(";")) {
			return true;
		}
		if(token.EqualsText(",") || token.EqualsText(")") || token.EqualsText("]") ||
				token.EqualsText("}") || token.EqualsText("&&") || token.EqualsText("||") || token.EqualsText("`")) {
			return true;
		}
		return false;
	}

	public static BNode toNode(BNode parentNode, BTokenContext tokenContext, BArray<BNode> nodeList) {
		BNode node = new BunStringNode(parentNode, null, "");
		int size = nodeList.size();
		for(int i = 0; i < size; i++) {
			BNode currentNode = BArray.GetIndex(nodeList, i);
			BunAddNode binaryNode = new BunAddNode(parentNode);
			binaryNode.SetLeftNode(node);
			binaryNode.SetRightNode(currentNode);
			node = binaryNode;
		}
		return node;
	}

	public static String resolveCommandPath(ArrayList<BToken> tokenList) {
		String commandPath = Utils.resolveHome(toCommandToken(tokenList).GetText());
		int index = commandPath.lastIndexOf('/');
		if(index != -1) {
			if(!Utils.isFileExecutable(commandPath)) {
				System.err.println("[warning] unknown command: " + commandPath);
				return "";
			}
		}
		else {
			String fullPath = Utils.getCommandFromPath(commandPath);
			if(fullPath == null) {
				System.err.println("[warning] unknown command: " + commandPath);
				return "";
			}
			commandPath = fullPath;
		}
		return commandPath;
	}

	public static BToken toCommandToken(ArrayList<BToken> tokenList) {
		if(tokenList.isEmpty()) {
			return BToken._NullToken;
		}
		int startIndex = tokenList.get(0).StartIndex;
		int endIndex = tokenList.get(tokenList.size() - 1).EndIndex;
		BToken commandToken = new BToken(tokenList.get(0).Source, startIndex, endIndex);
		tokenList.clear();
		return commandToken;
	}

	public static void checkDuplicationAndSetCommand(LibBunGamma gamma, String command, String commandPath) {
		LibBunSyntax syntax = gamma.GetSyntaxPattern(command);
		if(syntax != null) {
			if(LibBunSystem.DebugMode) {
				System.err.println("found duplicated syntax pattern: " + syntax);
			}
		}
		else if(!RuntimeContext.getContext().commandScope.setCommandPath(command, commandPath)) {
			if(LibBunSystem.DebugMode) {
				System.err.println("found duplicated symbol: " + command);
			}
		}
	}

	public static boolean matchPatternToken(BToken token, String patternName) {
		if(token instanceof BPatternToken) {
			BPatternToken patternToken = (BPatternToken) token;
			if(patternToken.PresetPattern.PatternName.equals(patternName)) {
				return true;
			}
		}
		return false;
	}

	public static void LoadGrammar(LibBunGamma gamma) {
		final BTokenFunction commandSymbolToken = new CommandTokenFunc();
		final BMatchFunction prefixOptionPattern = new PrefixOptionPatternFunc();

		gamma.DefineToken("#", new ShellStyleCommentTokenFunc());
		gamma.DefineToken("Aa_", commandSymbolToken);
		gamma.DefineToken("1", commandSymbolToken);
		gamma.DefineToken("~", commandSymbolToken);
		gamma.DefineToken(".", commandSymbolToken);
		gamma.DefineToken("/", commandSymbolToken);

		gamma.DefineStatement("import", new ImportPatternFunc());
		gamma.DefineExpression(ImportCommandPatternFunc.patternName, new ImportCommandPatternFunc());
		gamma.DefineExpression(ImportCommandAsPatternFunc.patternName, new ImportCommandAsPatternFunc());
		gamma.DefineExpression(CommandPatternFunc.patternName, new CommandPatternFunc());
		gamma.DefineExpression(CommandArgPatternFunc.patternName, new CommandArgPatternFunc());
		gamma.DefineExpression(RedirectPatternFunc.patternName, new RedirectPatternFunc());
		gamma.DefineExpression(PrefixOptionPatternFunc.patternName, prefixOptionPattern);
		gamma.DefineExpression(SuffixOptionPatternFunc._PatternName, new SuffixOptionPatternFunc());

		gamma.DefineStatement(ImportEnvPatternFunc.patternName, new ImportEnvPatternFunc());
		gamma.DefineStatement(ExportEnvPatternFunc.patternName, new ExportEnvPatternFunc());

		// from BultinCommandMap
		ArrayList<String> symbolList = BuiltinSymbol.getCommandSymbolList();
		for(String symbol : symbolList) {
			RuntimeContext.getContext().commandScope.setCommandPath(symbol);
		}
		// prefix option
		RuntimeContext.getContext().commandScope.setCommandPath(ShellGrammar.timeout);
		RuntimeContext.getContext().commandScope.setCommandPath(ShellGrammar.trace);
	}
}
