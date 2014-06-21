package dshell.internal.process;

import java.util.ArrayList;
import java.util.List;

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

	public boolean isCommandSymbol() {
		return true;
	}

	public static boolean match(String symbol) {
		try {
			BuiltinSymbol.valueOf(symbol);
			return true;
		} catch(IllegalArgumentException e) {
		}
		return false;
	}

	public static List<String> getCommandSymbolList() {
		List<String> symbolList = new ArrayList<String>();
		for(BuiltinSymbol symbol : BuiltinSymbol.values()) {
			if(symbol.isCommandSymbol()) {
				symbolList.add(symbol.name());
			}
		}
		return symbolList;
	}
}
