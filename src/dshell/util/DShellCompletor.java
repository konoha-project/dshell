package dshell.util;

import java.io.File;
import java.util.List;
import java.util.TreeSet;

import jline.Completor;

public class DShellCompletor implements Completor {
	private jline.SimpleCompletor commandCompletor;
	private jline.FileNameCompletor fileNameCompletor;
	private jline.ArgumentCompletor.ArgumentDelimiter delimiter;

	public DShellCompletor() {
		this.commandCompletor = new jline.SimpleCompletor("dummy");
		commandCompletor.setCandidates(getCommandListFromPath());
		this.fileNameCompletor = new jline.FileNameCompletor();
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
		jline.Completor completor = this.getCompletor(argIndex, argList.getArguments());
		int ret = completor.complete(argList.getCursorArgument(), argPos, candidates);
		if(ret == -1) {
			return -1;
		}
		return ret + (argList.getBufferPosition() - argPos);
	}

	private TreeSet<String> getCommandListFromPath() {
		TreeSet<String> commandSet = new TreeSet<String>();
		String[] paths = System.getenv("PATH").split(":");
		for(int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if(path.startsWith("~")) {
				path = System.getenv("HOME") + path.substring(1);
			}
			File file = new File(path);
			File[] files = file.listFiles();
			for(int j = 0; j < files.length; j++) {
				commandSet.add(files[j].getName());
			}
		}
		return commandSet;
	}

	private jline.Completor getCompletor(int argIndex, String[] args) {
		if(argIndex == 0) {
			return this.commandCompletor;
		}
		String prevArg = args[argIndex - 1];
		if(prevArg.equals("|") || prevArg.equals(">") || prevArg.equals("&&")) {
			return this.commandCompletor;
		}
		return this.fileNameCompletor;
	}
}
