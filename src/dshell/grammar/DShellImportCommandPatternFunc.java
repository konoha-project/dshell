package dshell.grammar;

import dshell.lib.Utils;
import libbun.lang.bun.shell.ImportCommandPatternFunction;

public class DShellImportCommandPatternFunc extends ImportCommandPatternFunction {

	@Override
	public String ResolveHome(String Path) {
		return Utils.resolveHome(Path);
	}

	@Override
	public boolean IsFileExecutable(String Path) {
		return Utils.isFileExecutable(Path);
	}

	@Override
	public String GetUnixCommand(String cmd) {
		return Utils.getCommandFromPath(cmd);
	}
}
