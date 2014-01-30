package dshell.lib;

import java.util.ArrayList;

import dshell.lib.BuiltinCommandMap.BuiltinCommandSymbol;
import dshell.util.LoggingContext;

interface CLibraryWrapper extends com.sun.jna.Library {
	CLibraryWrapper INSTANCE = (CLibraryWrapper) com.sun.jna.Native.loadLibrary("c", CLibraryWrapper.class);
	
	int chdir(String path);
	String getcwd(byte[] buf, int size);
	void perror(String s); 
}

public class BuiltinCommandMap {
	public static enum BuiltinCommandSymbol {
		assertResult {	// not builtin command
			@Override
			public String getUsage() {
				return "assert [bool_expr]";
			}

			@Override
			public String getDetail() {
				return "    Assertion statement in dshell.  If BOOL_EXPR is true, the " + "\n" +
					   "    statement will pass normaly.  If BOOL_EXPR is false, the " + "\n" +
					   "    statement will fail with displaying `AssertionError'.";
			}

			@Override
			public String getExternalName() {
				return "assert";
			}
		},
		cd {
			@Override
			public String getUsage() {
				return "cd [dir]";
			}

			@Override
			public String getDetail() {
				return "    Changing the current directory to DIR. The Environment variable" + "\n" +
					   "    HOME is the default DIR.  A null directory name is the same as " + "\n" +
					   "    the current directory.";
			}
		},
		exit {
			@Override
			public String getUsage() {
				return "exit [n]";
			}
			
			@Override
			public String getDetail() {
				return "    Exit the shell with a status of N.  If N is omitted, the exit  " + "\n" +
					   "    status is 0.";
			}
		},
		help {
			@Override
			public String getUsage() {
				return "help [-s] [pattern ...]";
			}
			
			@Override
			public String getDetail() {
				return "    Display helpful information about builtin commands.";
			}
		},
		log {
			@Override
			public String getUsage() {
				return "log [argument ...]";
			}
			
			@Override
			public String getDetail() {
				return "    Display arguments.  If shell option --logging:stout or " + "\n" +
					   "    --logging:stderr is set, display arguments to stdout or " + "\n" +
					   "    stderr.  If shell option --logging:syslog is set, write " + "\n" +
					   "    arguments to syslog.";
			}
		};

		public String getExternalName() {
			return this.name();
		}

		public String getUsage() {
			return "currently not defined";
		}

		public String getDetail() {
			return "    currently not defined";
		}

		public static boolean match(String symbol) {
			BuiltinCommandSymbol[] symbols = BuiltinCommandSymbol.values();
			for(BuiltinCommandSymbol currentSymbol : symbols) {
				if(currentSymbol.getExternalName().equals(symbol)) {
					return true;
				}
			}
			return false;
		}

		public static BuiltinCommandSymbol valueOfSymbol(String symbol) {
			BuiltinCommandSymbol[] symbols = BuiltinCommandSymbol.values();
			for(BuiltinCommandSymbol currentSymbol : symbols) {
				if(currentSymbol.getExternalName().equals(symbol)) {
					return currentSymbol;
				}
			}
			throw new IllegalArgumentException("Illegal Symbol: " + symbol);
		}
	}

	public static ArrayList<String> getCommandSymbolList() {
		ArrayList<String> symbolList = new ArrayList<String>();
		for(BuiltinCommandSymbol symbol : BuiltinCommandSymbol.values()) {
			symbolList.add(symbol.name());
		}
		return symbolList;
	}

	public static BuiltinCommand createCommand(ArrayList<String> cmds) {
		BuiltinCommand command = null;
		String commandSymbol = cmds.get(0);
		if(BuiltinCommandSymbol.match(commandSymbol)) {
			try {
				Class<?> classObject = Class.forName("dshell.lib.Command_" + commandSymbol);
				command = (BuiltinCommand) classObject.newInstance();
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

	public static int changeDirectory(String path) {
		String targetPath = path;
		if(path.equals("")) {
			targetPath = System.getenv("HOME");
		}
		return CLibraryWrapper.INSTANCE.chdir(targetPath);
	}

	public static String getWorkingDirectory() {
		int size = 256;
		byte[] buffers = new byte[size];
		CLibraryWrapper.INSTANCE.getcwd(buffers, size);
		int actualSize = 0;
		for(byte buf : buffers) {
			if(buf == 0) {
				break;
			}
			actualSize++;
		}
		byte[] newBuffers = new byte[actualSize];
		for(int i = 0; i < actualSize; i++) {
			newBuffers[i] = buffers[i];
		}
		return new String(newBuffers);
	}
}

abstract class BuiltinCommand extends PseudoProcess {
	@Override
	public void mergeErrorToOut() {
	}	// do nothing

	@Override
	public void setInputRedirect(String readFileName) {
	}	// do nothing

	@Override
	public void setOutputRedirect(int fd, String writeFileName, boolean append) {
	}	// do nothing

	abstract public void start();

	@Override
	public void kill() { // do nothing
	}

	@Override
	public void waitTermination() { // do nothing
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
		this.retValue = BuiltinCommandMap.changeDirectory(path);
		if(this.retValue == -1) {
			CLibraryWrapper.INSTANCE.perror("-dshell: cd");
		}
	}
}

class Command_exit extends BuiltinCommand {
	@Override
	public void start() {
		int status = 0;
		int size = this.commandList.size();
		if(size > 1) {
			try {
				status = Integer.parseInt(this.commandList.get(1));
			}
			catch(NumberFormatException e) {
			}
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
				if(BuiltinCommandSymbol.match(arg)) {
					foundValidCommand = true;
					BuiltinCommandSymbol symbol = BuiltinCommandSymbol.valueOfSymbol(arg);
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
		BuiltinCommandSymbol[] symbols = BuiltinCommandSymbol.values();
		for(BuiltinCommandSymbol symbol : symbols) {
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
		if(size == 1) {
			return;
		}
		StringBuilder sBuilder = new StringBuilder();
		for(int i = 1; i < size; i++) {
			if(i != 1) {
				sBuilder.append(" ");
			}
			sBuilder.append(this.commandList.get(i));
		}
		System.out.println(sBuilder.toString());
		LoggingContext.getContext().getLogger().warn(sBuilder.toString());
	}
}