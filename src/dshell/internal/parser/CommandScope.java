package dshell.internal.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import dshell.internal.lib.Utils;
import dshell.internal.process.BuiltinSymbol;

/**
 * represent shell command scope.
 * @author skgchxngsxyz-opensuse
 *
 */
public class CommandScope {
	private final Stack<LocalScope> scopeStack;

	public CommandScope() {
		this.scopeStack = new Stack<>();
		this.scopeStack.push(new GlobalScope());
	}

	/**
	 * resolve full path of command from PATH and set command.
	 * @param commandName
	 * @return
	 * - if has already defined, return false.
	 */
	public boolean setCommandPath(String commandName) {
		if(commandName == null) {
			return false;
		}
		String commandPath = Utils.getCommandFromPath(commandName);
		if(commandPath == null) {
			return false;
		}
		return this.setCommandPath(commandName, commandPath);
	}

	/**
	 * set command path to scope.
	 * @param commandName
	 * - command name (not contains /)
	 * @param commandPath
	 * - full path of command.
	 * @return
	 * - return false, command path has already defined.
	 */
	public boolean setCommandPath(String commandName, String commandPath) {
		if(commandName == null) {
			return false;
		}
		if(commandName.equals("") || commandPath.equals("")) {
			return true;
		}
		return this.scopeStack.peek().setCommandPath(commandName, commandPath);
	}

	/**
	 * check command name whether is acceptable symbol.
	 * @param commandName
	 * @return
	 * - if command name is builtin command, return always true,
	 * if name is qualified command (isQualifiedCommandName() == true), return true.
	 * if name is found in command scope, return true.
	 */
	public boolean isCommand(String commandName) {
		if(this.isQualifiedCommandName(commandName)) {
			return true;
		}
		return this.scopeStack.peek().isCommand(commandName);
	}

	/**
	 * resolve qualified command path.
	 * @param commandName
	 * @return
	 * - return null if has no command path.
	 */
	public String resolveCommandPath(String commandName) {
		if(this.isQualifiedCommandName(commandName)) {
			return Utils.resolveHome(commandName);
		}
		return this.scopeStack.peek().getCommandPath(commandName);
	}

	public void createNewScope() {
		this.scopeStack.push(new LocalScope(this.scopeStack.peek()));
	}

	public void removeCurrentScope() {
		if(this.scopeStack.size() > 1) {
			this.scopeStack.pop();
		}
	}

	private boolean isQualifiedCommandName(String commandName) {
		int size = commandName.length();
		if(commandName.startsWith("./") && size > 2) {
			return true;
		}
		if(commandName.startsWith("~/") && size > 2) {
			return true;
		}
		if(commandName.startsWith("/") && size > 1) {
			return true;
		}
		return false;
	}

	private static class LocalScope {
		private final LocalScope parentScope;
		protected final Map<String, String> commandMap;

		private LocalScope(LocalScope parentScope) {
			this.parentScope = parentScope;
			this.commandMap = new HashMap<>();
		}

		private LocalScope getParentScope() {
			return this.parentScope;
		}

		/**
		 * add command to scope.
		 * @param commandName
		 * - command name
		 * @param commandPath
		 * - full path of command.
		 * @return
		 * - if command has already defined, return false.
		 */
		public boolean setCommandPath(String commandName, String commandPath) {
			if(this.commandMap.containsKey(commandName)) {
				return false;
			}
			this.commandMap.put(commandName, commandPath);
			return true;
		}

		/**
		 * 
		 * @param commandName
		 * @return
		 * - if is command, return true.
		 */
		public boolean isCommand(String commandName) {
			if(this.commandMap.containsKey(commandName)) {
				return true;
			}
			if(this.getParentScope() == null) {
				return false;
			}
			return this.getParentScope().isCommand(commandName);
		}

		/**
		 * get path of target command.
		 * @param commandName
		 * @return
		 * - return null, if has no command.
		 */
		public String getCommandPath(String commandName) {
			String Command = this.commandMap.get(commandName);
			if(Command != null) {
				return Command;
			}
			if(this.getParentScope() == null) {
				return null;
			}
			return this.getParentScope().getCommandPath(commandName);
		}
	}

	private static class GlobalScope extends LocalScope {
		private GlobalScope() {
			super(null);
			List<String> builtinCommands = BuiltinSymbol.getCommandSymbolList();
			for(String commandName : builtinCommands) {
				this.setCommandPath(commandName, commandName);
			}
		}

		@Override
		public boolean setCommandPath(String commandName, String commandPath) {
			if(this.commandMap.containsKey(commandName)) {
				return false;
			}
			this.commandMap.put(commandName, Utils.resolveHome(commandPath));
			return true;
		}

		@Override
		public boolean isCommand(String commandName) {
			return this.commandMap.containsKey(commandName);
		}

		@Override
		public String getCommandPath(String commandName) {
			return this.commandMap.get(commandName);
		}
	}
}
