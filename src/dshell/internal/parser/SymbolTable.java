package dshell.internal.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import dshell.internal.parser.TypePool.Type;
import dshell.internal.parser.SymbolTable.SymbolEntry;

/**
 * contains defined variable and function symbol type.
 * @author skgchxngsxyz-opensuse
 *
 */

interface SymbolTableOp {
	/**
	 * get entry from it.
	 * @param symbolName
	 * @return
	 * if entry does not exist, search parent symbol table.
	 * return null, if entry does not exist.
	 */
	public SymbolEntry getEntry(String symbolName);

	/**
	 * add new entry
	 * @param symbolName
	 * @param type
	 * @param isReadOnly
	 * @return
	 * - if entry has already existed , return false.
	 */
	public boolean addEntry(String symbolName, Type type, boolean isReadOnly);
}

public class SymbolTable implements SymbolTableOp {
	private final Stack<SymbolTableOp> tableStack;

	public SymbolTable() {
		this.tableStack = new Stack<>();
		this.tableStack.push(new ChildTable(new EmptyTable()));
	}

	@Override
	public SymbolEntry getEntry(String symbolName) {
		return this.tableStack.peek().getEntry(symbolName);
	}

	@Override
	public boolean addEntry(String symbolName, Type type, boolean isReadOnly) {
		return this.tableStack.peek().addEntry(symbolName, type, isReadOnly);
	}

	/**
	 * create child symbol table.
	 * child table contains reference of this table.
	 * @return
	 */
	public void createAndPushNewTable() {
		this.tableStack.push(new ChildTable(this.tableStack.peek()));
	}

	/**
	 * remove current symbol table.
	 */
	public void popCurrentTable() {
		if(this.tableStack.size() > 1) {
			this.tableStack.pop();
		}
	}

	private static class ChildTable implements SymbolTableOp {
		/**
		 * parent symbol table reference.
		 */
		private final SymbolTableOp parentTable;

		/**
		 * contain symbol entry.
		 * key is symbol name.
		 */
		private final Map<String, SymbolEntry> entryMap;

		private ChildTable(SymbolTableOp parentTable) {
			this.parentTable = parentTable;
			this.entryMap = new HashMap<>();
		}

		private SymbolTableOp getParentTable() {
			return this.parentTable;
		}

		@Override
		public SymbolEntry getEntry(String symbolName) {
			SymbolEntry entry = this.entryMap.get(symbolName);
			if(entry == null) {
				return this.getParentTable().getEntry(symbolName);
			}
			return entry;
		}

		@Override
		public boolean addEntry(String symbolName, Type type, boolean isReadOnly) {
			if(this.entryMap.containsKey(symbolName)) {
				return false;
			}
			boolean isGlobal = (this.getParentTable() instanceof EmptyTable);
			SymbolEntry entry = new SymbolEntry(type, isReadOnly, isGlobal);
			this.entryMap.put(symbolName, entry);
			return true;
		}
	}

	private static class EmptyTable implements SymbolTableOp {
		@Override
		public SymbolEntry getEntry(String symbolName) {
			return null;
		}

		@Override
		public boolean addEntry(String symbolName, Type type, boolean isReadOnly) {
			return false;	// do not call this.
		}
	}

	public static class SymbolEntry {
		/**
		 * represent read only symbol (constant variable, function).
		 */
		private final boolean isReadOnly;
		private final boolean isGlobal;
		private final Type type;

		private SymbolEntry(Type type, boolean isReadOnly, boolean isGlobal) {
			this.type = type;
			this.isReadOnly = isReadOnly;
			this.isGlobal = isGlobal;
		}

		public Type getType() {
			return this.type;
		}

		public boolean isReadOnly() {
			return this.isReadOnly;
		}

		public boolean isGlobal() {
			return this.isGlobal;
		}
	}
}
