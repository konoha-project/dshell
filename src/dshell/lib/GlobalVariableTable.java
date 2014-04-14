package dshell.lib;

import java.util.Arrays;
import java.util.HashMap;

public class GlobalVariableTable {
	private final static int MAX_INDEX = Integer.MAX_VALUE;
	private final static int defaultTableSize = 16;
	public final static int LONG_TYPE    = 0;
	public final static int DOUBLE_TYPE  = 1;
	public final static int BOOLEAN_TYPE = 2;
	public final static int OBJECT_TYPE  = 3;
	private final static int TYPE_ID_SIZE = 4;

	private final HashMap<String, TableEntry> varIndexMap;
	private final int[] varIndexCounter;
	private long[] longVarTable;
	private double[] doubleVarTable;
	private boolean[] booleanVarTable;
	private Object[] objectVarTable;

	protected GlobalVariableTable() {
		this.varIndexMap = new HashMap<String, TableEntry>();
		this.varIndexCounter = new int[TYPE_ID_SIZE];
		this.longVarTable = new long[defaultTableSize];
		this.doubleVarTable = new double[defaultTableSize];
		this.booleanVarTable = new boolean[defaultTableSize];
		this.objectVarTable = new Object[defaultTableSize];
	}

	public int addEntry(String varName, int typeId, boolean isReadOnly) {
		if(this.existEntry(varName)) {
			return -1;
		}
		int varIndex;
		switch(typeId) {
		case LONG_TYPE:
			varIndex = this.reserveLongVarTable();
			break;
		case DOUBLE_TYPE:
			varIndex = this.reserveDoubleVarTable();
			break;
		case BOOLEAN_TYPE:
			varIndex = this.reserveBooleanVarTable();
			break;
		case OBJECT_TYPE:
			varIndex = this.reserveObjectVarTable();
			break;
		default:
			throw new RuntimeException("invalid type id: " + typeId);
		}
		this.varIndexMap.put(varName, new TableEntry(typeId, varIndex, isReadOnly));
		return varIndex;
	}

	public boolean existEntry(String varName) {
		return this.varIndexMap.containsKey(varName);
	}

	public boolean isReadOnlyEntry(String varName) {
		TableEntry entry = this.varIndexMap.get(varName);
		if(entry == null) {
			throw new IllegalArgumentException("not found entry: " + varName);
		}
		return entry.isReadOnly;
	}

	public int getVarIndex(String varName, int typeId) {
		TableEntry entry = this.varIndexMap.get(varName);
		if(entry != null && entry.typeId == typeId) {
			return entry.varIndex;
		}
		return -1;
	}

	// reserve variable table
	private int reserveLongVarTable() {
		final int size = this.longVarTable.length;
		if(++this.varIndexCounter[LONG_TYPE] == size) {
			this.checkIndexRange(size);
			this.longVarTable = Arrays.copyOf(this.longVarTable, size * 2);
		}
		return this.varIndexCounter[LONG_TYPE];
	}

	private int reserveDoubleVarTable() {
		final int size = this.doubleVarTable.length;
		if(++this.varIndexCounter[DOUBLE_TYPE] == size) {
			this.checkIndexRange(size);
			this.doubleVarTable = Arrays.copyOf(this.doubleVarTable, size * 2);
		}
		return this.varIndexCounter[DOUBLE_TYPE];
	}

	private int reserveBooleanVarTable() {
		final int size = this.booleanVarTable.length;
		if(++this.varIndexCounter[BOOLEAN_TYPE] == size) {
			this.checkIndexRange(size);
			this.booleanVarTable = Arrays.copyOf(this.booleanVarTable, size * 2);
		}
		return this.varIndexCounter[BOOLEAN_TYPE];
	}

	private int reserveObjectVarTable() {
		final int size = this.objectVarTable.length;
		if(++this.varIndexCounter[OBJECT_TYPE] == size) {
			this.checkIndexRange(size);
			this.objectVarTable = Arrays.copyOf(this.objectVarTable, size * 2);
		}
		return this.varIndexCounter[OBJECT_TYPE];
	}

	private void checkIndexRange(int size) {
		if(size >= MAX_INDEX) {
			throw new RuntimeException("too many global variable");
		}
	}

	// called by DShellByteCodeGenerator
	// for long variable
	public static void setLongVariable(int index, long value) {
		TableHolder.table.longVarTable[index] = value;
	}

	public static long getLongVariable(int index) {
		return TableHolder.table.longVarTable[index];
	}

	// for double variable
	public static void setDoubleVariable(int index, double value) {
		TableHolder.table.doubleVarTable[index] = value;
	}

	public static double getDoubleVariable(int index) {
		return TableHolder.table.doubleVarTable[index];
	}

	// for boolean variable
	public static void setBooleanVariable(int index, boolean value) {
		TableHolder.table.booleanVarTable[index] = value;
	}

	public static boolean getBooleanVariable(int index) {
		return TableHolder.table.booleanVarTable[index];
	}

	// for object variable
	public static void setObjectVariable(int index, Object value) {
		TableHolder.table.objectVarTable[index] = value;
	}

	public static Object getObjectVariable(int index) {
		return TableHolder.table.objectVarTable[index];
	}

	private static class TableEntry {
		public final boolean isReadOnly;
		public final int typeId;
		public final int varIndex;

		public TableEntry(int typeId, int varIndex, boolean isReadOnly) {
			this.typeId = typeId;
			this.varIndex = varIndex;
			this.isReadOnly = isReadOnly;
		}
	}

	private static class TableHolder {
		private final static GlobalVariableTable table = new GlobalVariableTable();
	}

	public static GlobalVariableTable getVarTable() {
		return TableHolder.table;
	}
}
