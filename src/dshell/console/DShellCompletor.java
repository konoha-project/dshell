package dshell.console;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import dshell.grammar.ShellGrammar;
import dshell.lib.BuiltinCommand;
import dshell.lib.RuntimeContext;
import dshell.lib.Utils;
import jline.Completor;

public class DShellCompletor implements Completor {
	private jline.SimpleCompletor commandCompletor;
	private jline.SimpleCompletor envCompletor;
	private jline.SimpleCompletor importCompletor;
	private DShellFileNameCompletor fileNameCompletor;
	private jline.ArgumentCompletor.ArgumentDelimiter delimiter;

	public DShellCompletor() {
		this.commandCompletor = new jline.SimpleCompletor("dummy");
		this.commandCompletor.setCandidates(getCommandSet());
		this.envCompletor = new jline.SimpleCompletor("dummy");
		this.envCompletor.setCandidates(RuntimeContext.getContext().getEnvSet());
		this.importCompletor = new jline.SimpleCompletor(new String[]{"command", "env"});
		this.fileNameCompletor = new DShellFileNameCompletor();
		this.delimiter = new jline.ArgumentCompletor.WhitespaceArgumentDelimiter();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public int complete(String buffer, int cursor, List candidates) {
		jline.ArgumentCompletor.ArgumentList argList = this.delimiter.delimit(buffer, cursor);
		int argPos = argList.getArgumentPosition();
		int argIndex = argList.getCursorArgumentIndex();
		if(argIndex < 0) {
			return -1;
		}
		jline.Completor completor = this.selectCompletor(argIndex, argList.getArguments());
		int ret = completor.complete(argList.getCursorArgument(), argPos, candidates);
		if(ret == -1) {
			return -1;
		}
		return ret + argList.getBufferPosition() - argPos;
	}

	private static TreeSet<String> getCommandSet() {
		TreeSet<String> commandSet = Utils.getCommandSetFromPath();
		// add builtin command
		ArrayList<String> symbolList = BuiltinCommand.getCommandSymbolList();
		for(String symbol : symbolList) {
			commandSet.add(symbol);
		}
		return commandSet;
	}

	private jline.Completor selectCompletor(int argIndex, String[] args) {
		if(this.isCommandRequired(argIndex, args)) {
			return this.commandCompletor;
		}
		String prevArg = args[argIndex - 1];
		if(prevArg.equals("env") && argIndex - 2 > -1 && args[argIndex - 2].equals("import")) {
			return this.envCompletor;
		}
		if(prevArg.equals("import")) {
			return this.importCompletor;
		}
		return this.fileNameCompletor;
	}

	private boolean isCommandRequired(int argIndex, String[] args) {
		if(argIndex == 0) {
			return true;
		}
		String prevArg = args[argIndex - 1];
		if(prevArg.equals(ShellGrammar.timeout) || prevArg.equals(ShellGrammar.trace) 
				|| prevArg.equals("command")) {
			return true;
		}
		if(prevArg.equals("|") || prevArg.equals("&&") || prevArg.equals("||") || prevArg.equals(";")) {
			return true;
		}
		return false;
	}
}
