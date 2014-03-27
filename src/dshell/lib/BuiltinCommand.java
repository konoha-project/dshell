package dshell.lib;

import java.util.ArrayList;

public abstract class BuiltinCommand extends PseudoProcess {
	@Override
	public void mergeErrorToOut() {
	}	// do nothing
	
	@Override
	public void setInputRedirect(CommandArg readFileName) {
	}	// do nothing
	
	@Override
	public void setOutputRedirect(int fd, CommandArg writeFileName, boolean append) {
	}	// do nothing
	
	abstract public void start();
	
	@Override
	public void kill() { // do nothing
	}
	
	@Override
	public void waitTermination() { // do nothing
	}

	@Override
	public boolean checkTermination() {
		return true;
	}

	protected void printArgumentErrorAndSetStatus(BuiltinSymbol symbol) {
		System.err.println("-dshell: " + symbol.name() + ": invalid argument");
		System.err.println(symbol.name() + ": " + symbol.getUsage());
		this.retValue = 1;
	}

	public static BuiltinCommand createCommand(ArrayList<CommandArg> cmds) {
		String commandSymbol = cmds.get(0).toString();
		boolean matchCommand = false;
		ArrayList<String> commandSymbolList = getCommandSymbolList();
		for(String currentSymbol : commandSymbolList) {
			if(currentSymbol.equals(commandSymbol)) {
				matchCommand = true;
				break;
			}
		}
		if(matchCommand) {
			try {
				Class<?> classObject = Class.forName("dshell.lib.Command_" + commandSymbol);
				BuiltinCommand command = (BuiltinCommand) classObject.newInstance();
				command.setArgumentList(cmds);
				return command;
			}
			catch (ClassNotFoundException e) {	
			}
			catch(InstantiationException e) {
			}
			catch (IllegalAccessException e) {
			}
		}
		return null;
	}

	public static ArrayList<String> getCommandSymbolList() {
		ArrayList<String> symbolList = new ArrayList<String>();
		for(BuiltinSymbol symbol : BuiltinSymbol.values()) {
			if(symbol.isCommandSymbol()) {
				symbolList.add(symbol.getExternalName());
			}
		}
		return symbolList;
	}
}

class Command_cd extends BuiltinCommand {
	@Override
	public void start() {
		int size = this.commandList.size();
		String path = "";
		if(size > 1) {
			path = this.commandList.get(1);
		}
		this.retValue = RuntimeContext.getContext().changeDirectory(path);
	}
}

class Command_exit extends BuiltinCommand {
	@Override
	public void start() {
		int status;
		int size = this.commandList.size();
		if(size == 1) {
			status = 0;
		}
		else if(size == 2) {
			try {
				status = Integer.parseInt(this.commandList.get(1));
			}
			catch(NumberFormatException e) {
				this.printArgumentErrorAndSetStatus(BuiltinSymbol.exit);
				return;
			}
		}
		else {
			this.printArgumentErrorAndSetStatus(BuiltinSymbol.exit);
			return;
		}
		System.exit(status);
	}
}

class Command_help extends BuiltinCommand {
	@Override
	public void start() {
		int size = this.commandList.size();
		boolean foundValidCommand = false;
		boolean isShortHelp = false;
		if(size == 1) {
			this.printAllCommandUsage();
			foundValidCommand = true;
		}
		for(int i = 1; i < size; i++) {
			String arg = this.commandList.get(i);
			if(arg.equals("-s") && size == 2) {
				this.printAllCommandUsage();
				foundValidCommand = true;
			}
			else if(arg.equals("-s") && i == 1) {
				isShortHelp = true;
			}
			else {
				if(BuiltinSymbol.match(arg)) {
					foundValidCommand = true;
					BuiltinSymbol symbol = BuiltinSymbol.valueOfSymbol(arg);
					System.out.println(arg + ": " + symbol.getUsage());
					if(!isShortHelp) {
						System.out.println(symbol.getDetail());
					}
				}
			}
		}
		if(!foundValidCommand) {
			this.printNotMatchedMessage(this.commandList.get(size - 1));
		}
		this.retValue = foundValidCommand ? 0 : 1;
	}

	private void printAllCommandUsage() {
		BuiltinSymbol[] symbols = BuiltinSymbol.values();
		for(BuiltinSymbol symbol : symbols) {
			System.out.println(symbol.getUsage());
		}
	}

	private void printNotMatchedMessage(String commandSymbol) {
		System.err.println("-dshell: help: not no help topics match `" + commandSymbol + "'.  Try `help help'.");
	}
}

class Command_log extends BuiltinCommand {
	@Override
	public void start() {
		int size = this.commandList.size();
		if(size != 2) {
			this.printArgumentErrorAndSetStatus(BuiltinSymbol.log);
			return;
		}
		Utils.log(this.commandList.get(1).toString());
		this.retValue = 0;
	}
}
