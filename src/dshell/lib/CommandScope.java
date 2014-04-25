package dshell.lib;

import java.util.HashMap;
import java.util.Stack;

interface CommandScopeOp {
	public boolean setCommand(String commandSymbol, String command);
	public boolean isCommand(String commandSymbol);
	public String getCommand(String commandSymbol);
}

public class CommandScope implements CommandScopeOp {
	private final Stack<LocalScope> scopeStack;

	public CommandScope() {
		this.scopeStack = new Stack<CommandScope.LocalScope>();
		this.scopeStack.push(new LocalScope());
	}

	@Override
	public boolean setCommand(String commandSymbol, String command) {
		return this.scopeStack.peek().setCommand(commandSymbol, command);
	}

	@Override
	public boolean isCommand(String commandSymbol) {
		return this.scopeStack.peek().isCommand(commandSymbol);
	}

	@Override
	public String getCommand(String commandSymbol) {
		return this.scopeStack.peek().getCommand(commandSymbol);
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
		public boolean setCommand(String commandSymbol, String command) {
			if(this.commandMap.containsKey(commandSymbol)) {
				return false;
			}
			this.commandMap.put(commandSymbol, command);
			return true;
		}

		@Override
		public boolean isCommand(String commandSymbol) {
			if(this.commandMap.containsKey(commandSymbol)) {
				return true;
			}
			if(this.getParentScope() == null) {
				return false;
			}
			return this.getParentScope().isCommand(commandSymbol);
		}

		@Override
		public String getCommand(String commandSymbol) {
			String Command = this.commandMap.get(commandSymbol);
			if(Command != null) {
				return Command;
			}
			if(this.getParentScope() == null) {
				return null;
			}
			return this.getParentScope().getCommand(commandSymbol);
		}
	}
}
