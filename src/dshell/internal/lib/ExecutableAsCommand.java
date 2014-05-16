package dshell.internal.lib;

import java.util.ArrayList;

public interface ExecutableAsCommand {
	public void execute(CommandContext context, ArrayList<String> argList);
}
