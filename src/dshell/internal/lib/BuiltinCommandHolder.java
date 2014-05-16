package dshell.internal.lib;

import java.util.ArrayList;
import java.util.HashMap;

public class BuiltinCommandHolder {
	private final HashMap<BuiltinSymbol, ExecutableAsCommand> builtinCommandMap;

	public BuiltinCommandHolder() {
		this.builtinCommandMap = new HashMap<BuiltinSymbol, ExecutableAsCommand>();
		this.builtinCommandMap.put(BuiltinSymbol.cd, new Command_cd());
		this.builtinCommandMap.put(BuiltinSymbol.exit, new Command_exit());
		this.builtinCommandMap.put(BuiltinSymbol.help, new Command_help());
		this.builtinCommandMap.put(BuiltinSymbol.log, new Command_log());
	}

	public CommandRunner createCommand(ArrayList<CommandArg> cmds) {
		String commandSymbol = cmds.get(0).toString();
		try {
			BuiltinSymbol symbol = BuiltinSymbol.valueOfSymbol(commandSymbol);
			ExecutableAsCommand executor = this.builtinCommandMap.get(symbol);
			if(executor == null) {
				return null;
			}
			CommandRunner runner = new CommandRunner(executor);
			runner.setArgumentList(cmds);
			return runner;
		}
		catch(IllegalArgumentException e) {
		}
		return null;
	}

	public static void printArgumentErrorAndSetStatus(BuiltinSymbol symbol, CommandContext context) {
		StreamUtils.OutputStream output = context.getStderr();
		output.writeLine("-dshell: " + symbol.name() + ": invalid argument");
		output.writeLine(symbol.name() + ": " + symbol.getUsage());
		context.setExistStatus(1);
	}

	public static class Command_cd implements ExecutableAsCommand {
		@Override
		public void execute(CommandContext context, ArrayList<String> argList) {
			int size = argList.size();
			String path = "";
			if(size > 1) {
				path = argList.get(1);
			}
			context.setExistStatus(RuntimeContext.getContext().changeDirectory(path));
		}
	}

	public static class Command_exit implements ExecutableAsCommand {
		@Override
		public void execute(CommandContext context, ArrayList<String> argList) {
			int status;
			int size = argList.size();
			if(size == 1) {
				status = 0;
			}
			else if(size == 2) {
				try {
					status = Integer.parseInt(argList.get(1));
				}
				catch(NumberFormatException e) {
					printArgumentErrorAndSetStatus(BuiltinSymbol.exit, context);
					return;
				}
			}
			else {
				printArgumentErrorAndSetStatus(BuiltinSymbol.exit, context);
				return;
			}
			System.exit(status);
		}
	}

	public static class Command_help implements ExecutableAsCommand {
		@Override
		public void execute(CommandContext context, ArrayList<String> argList) {
			StreamUtils.OutputStream stdout = context.getStdout();
			StreamUtils.OutputStream stderr = context.getStderr();
			int size = argList.size();
			boolean foundValidCommand = false;
			boolean isShortHelp = false;
			if(size == 1) {
				this.printAllCommandUsage(stdout);
				foundValidCommand = true;
			}
			for(int i = 1; i < size; i++) {
				String arg = argList.get(i);
				if(arg.equals("-s") && size == 2) {
					this.printAllCommandUsage(stdout);
					foundValidCommand = true;
				}
				else if(arg.equals("-s") && i == 1) {
					isShortHelp = true;
				}
				else {
					if(BuiltinSymbol.match(arg)) {
						foundValidCommand = true;
						BuiltinSymbol symbol = BuiltinSymbol.valueOfSymbol(arg);
						stdout.writeLine(arg + ": " + symbol.getUsage());
						if(!isShortHelp) {
							stdout.writeLine(symbol.getDetail());
						}
					}
				}
			}
			if(!foundValidCommand) {
				this.printNotMatchedMessage(argList.get(size - 1), stderr);
			}
			context.setExistStatus(foundValidCommand ? 0 : 1);
		}

		private void printAllCommandUsage(StreamUtils.OutputStream stdout) {
			BuiltinSymbol[] symbols = BuiltinSymbol.values();
			for(BuiltinSymbol symbol : symbols) {
				stdout.writeLine(symbol.getUsage());
			}
		}

		private void printNotMatchedMessage(String commandSymbol, StreamUtils.OutputStream stderr) {
			stderr.writeLine("-dshell: help: not no help topics match `" + commandSymbol + "'.  Try `help help'.");
		}
	}

	public static class Command_log implements ExecutableAsCommand {
		@Override
		public void execute(CommandContext context, ArrayList<String> argList) {
			int size = argList.size();
			if(size == 1) {
				this.log("", context.getStdout());
				context.setExistStatus(0);
				return;
			}
			if(size != 2) {
				printArgumentErrorAndSetStatus(BuiltinSymbol.log, context);
				return;
			}
			this.log(argList.get(1).toString(), context.getStdout());
			context.setExistStatus(0);
		}

		private void log(String value, StreamUtils.OutputStream stdout) {
			stdout.writeLine(value);
			RuntimeContext.getContext().getLogger().warn(value);
		}
	}
}
