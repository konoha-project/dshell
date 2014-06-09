package dshell.internal.process;

import java.util.ArrayList;

public enum BuiltinSymbol {
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
	log {	// not builtin command
		@Override
		public String getUsage() {
			return "log [symbol]";
		}
		@Override
		public String getDetail() {
			return "    Logging Command.  Display SYMBOL.  If shell option " + "\n" +
			       "    --logging:stdout or --logging:stderr is set, display value to " + "\n" +
			       "    stdout or stderr.  If shell option --logging:syslog is set, " + "\n" +
			       "    write value to syslog.";
		}
	};

	public String getUsage() {
		return "currently not defined";
	}

	public String getDetail() {
		return "    currently not defined";
	}

	public String getExternalName() {
		return this.name();
	}

	public boolean isCommandSymbol() {
		return true;
	}

	public static boolean match(String symbol) {
		BuiltinSymbol[] symbols = BuiltinSymbol.values();
		for(BuiltinSymbol currentSymbol : symbols) {
			if(currentSymbol.getExternalName().equals(symbol)) {
				return true;
			}
		}
		return false;
	}

	public static BuiltinSymbol valueOfSymbol(String symbol) {
		BuiltinSymbol[] symbols = BuiltinSymbol.values();
		for(BuiltinSymbol currentSymbol : symbols) {
			if(currentSymbol.getExternalName().equals(symbol)) {
				return currentSymbol;
			}
		}
		throw new IllegalArgumentException("Illegal Symbol: " + symbol);
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
