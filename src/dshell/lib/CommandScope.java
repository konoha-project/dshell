package dshell.lib;

import java.util.HashMap;
import java.util.Stack;

interface CommandScopeOp {
	public boolean setCommandPath(String commandName, String commandPath);
	public boolean isCommand(String commandName);
	public String getCommandPath(String commandName);
}

public class CommandScope implements CommandScopeOp {
	private final Stack<LocalScope> scopeStack;

	public CommandScope() {
		this.scopeStack = new Stack<CommandScope.LocalScope>();
		this.scopeStack.push(new LocalScope());
	}

	@Override
	public boolean setCommandPath(String commandName, String commandPath) {
		return this.scopeStack.peek().setCommandPath(commandName, commandPath);
	}

	@Override
	public boolean isCommand(String commandName) {
		return this.scopeStack.peek().isCommand(commandName);
	}

	@Override
	public String getCommandPath(String commandName) {
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

	private static class LocalScope implements CommandScopeOp {
		private final LocalScope parentScope;
		private final HashMap<String, String> commandMap;

		public LocalScope(LocalScope parentScope) {
			this.parentScope = parentScope;
			this.commandMap = new HashMap<String, String>();
		}

		public LocalScope() {
			this(null);
		}

		private LocalScope getParentScope() {
			return this.parentScope;
		}

		@Override
		public boolean setCommandPath(String commandName, String commandPath) {
			if(this.commandMap.containsKey(commandName)) {
				return false;
			}
			this.commandMap.put(commandName, commandPath);
			return true;
		}

		@Override
		public boolean isCommand(String commandName) {
			if(this.commandMap.containsKey(commandName)) {
				return true;
			}
			if(this.getParentScope() == null) {
				return false;
			}
			return this.getParentScope().isCommand(commandName);
		}

		@Override
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
}
