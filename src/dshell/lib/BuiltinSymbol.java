package dshell.lib;

public enum BuiltinSymbol {
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
		@Override
		public boolean isCommandSymbol() {
			return false;
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
	log {	// not builtin command
		@Override
		public String getUsage() {
			return "log [expr]";
		}
		@Override
		public String getDetail() {
			return "    Logging Statement.  Display value of EXPR.  If shell option " + "\n" +
				   "    --logging:stout or --logging:stderr is set, display value to " + "\n" +
				   "    stdout or stderr.  If shell option --logging:syslog is set, " + "\n" +
				   "    write value to syslog.";
		}
		@Override
		public String getExternalName() {
			return "log";
		}
		@Override
		public boolean isCommandSymbol() {
			return false;
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
}
