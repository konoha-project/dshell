package dshell.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import dshell.lang.DShellGrammar;
import dshell.lib.BuiltinCommand;
import jline.Completor;

public class DShellCompletor implements Completor {
	private jline.SimpleCompletor commandCompletor;
	private DShellFileNameCompletor fileNameCompletor;
	private jline.ArgumentCompletor.ArgumentDelimiter delimiter;

	public DShellCompletor() {
		this.commandCompletor = new jline.SimpleCompletor("dummy");
		commandCompletor.setCandidates(getCommandSet());
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
		if(argIndex == 0) {
			return this.commandCompletor;
		}
		String prevArg = args[argIndex - 1];
		if(prevArg.equals(DShellGrammar.timeout) || prevArg.equals(DShellGrammar.trace) 
				|| prevArg.equals("command") || prevArg.equals("import")) {
			return this.commandCompletor;
		}
		if(prevArg.equals("|") || prevArg.equals("&&") || prevArg.equals("||") || prevArg.equals(";")) {
			return this.commandCompletor;
		}
		return this.fileNameCompletor;
	}
}
