package dshell.internal.lib;

import java.util.List;

public interface ExecutableAsCommand {
	public void execute(CommandContext context, List<String> argList);
}
