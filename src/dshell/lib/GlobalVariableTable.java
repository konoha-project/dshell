package dshell.lib;

import java.util.HashMap;

public class GlobalVariableTable {
	private final HashMap<String, TableEntry> varMap;

	public GlobalVariableTable() {
		this.varMap = new HashMap<String, TableEntry>();
	}

	public void addEntry(String varName, Object value, boolean isReadOnly) {
		if(this.existEntry(varName)) {
			throw new IllegalArgumentException("duplicated entry: " + varName);
		}
		this.varMap.put(varName, new TableEntry(value, isReadOnly));
	}

	public void updateEntry(String varName, Object value) {
		if(this.isReadOnlyEntry(varName)) {
			throw new IllegalArgumentException("read only entry: " + varName);
		}
		this.varMap.get(varName).setValue(value);
	}

	public Object getValueOfEntry(String varName) {
		if(!this.existEntry(varName)) {
			throw new IllegalArgumentException("not found entry: " + varName);
		}
		return this.varMap.get(varName).getValue();
	}

	public boolean isReadOnlyEntry(String varName) {
		if(!this.existEntry(varName)) {
			throw new IllegalArgumentException("not found entry: " + varName);
		}
		return this.varMap.get(varName).isReadOnly;
	}

	public boolean existEntry(String varName) {
		return this.varMap.containsKey(varName);
	}

	private static class TableEntry {
		public final boolean isReadOnly;
		private Object value;

		public TableEntry(Object value, boolean isReadOnly) {
			this.value = value;
			this.isReadOnly = isReadOnly;
		}

		public boolean setValue(Object value) {
			if(this.isReadOnly) {
				return false;
			}
			this.value = value;
			return true;
		}

		public Object getValue() {
			return this.value;
		}
	}
}
