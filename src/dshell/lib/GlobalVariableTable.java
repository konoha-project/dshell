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

	private static final HashMap<String, TableEntry> varIndexMap;
	private static final int[] varIndexCounter;
	public static long[] longVarTable;
	public static double[] doubleVarTable;
	public static boolean[] booleanVarTable;
	public static Object[] objectVarTable;

	static {
		varIndexMap = new HashMap<String, TableEntry>();
		varIndexCounter = new int[TYPE_ID_SIZE];
		longVarTable = new long[defaultTableSize];
		doubleVarTable = new double[defaultTableSize];
		booleanVarTable = new boolean[defaultTableSize];
		objectVarTable = new Object[defaultTableSize];
	}

	public static int addEntry(String varName, int typeId, boolean isReadOnly) {
		if(existEntry(varName)) {
			return -1;
		}
		int varIndex;
		switch(typeId) {
		case LONG_TYPE:
			varIndex = reserveLongVarTable();
			break;
		case DOUBLE_TYPE:
			varIndex = reserveDoubleVarTable();
			break;
		case BOOLEAN_TYPE:
			varIndex = reserveBooleanVarTable();
			break;
		case OBJECT_TYPE:
			varIndex = reserveObjectVarTable();
			break;
		default:
			throw new RuntimeException("invalid type id: " + typeId);
		}
		varIndexMap.put(varName, new TableEntry(typeId, varIndex, isReadOnly));
		return varIndex;
	}

	public static void removeEntry(String varName) {
		varIndexMap.remove(varName);
	}

	public static boolean existEntry(String varName) {
		return varIndexMap.containsKey(varName);
	}

	public static boolean isReadOnlyEntry(String varName) {
		TableEntry entry = varIndexMap.get(varName);
		if(entry == null) {
			throw new IllegalArgumentException("not found entry: " + varName);
		}
		return entry.isReadOnly;
	}

	public static int getVarIndex(String varName, int typeId) {
		TableEntry entry = varIndexMap.get(varName);
		if(entry != null && entry.typeId == typeId) {
			return entry.varIndex;
		}
		return -1;
	}

	// reserve variable table
	private static int reserveLongVarTable() {
		final int size = longVarTable.length;
		if(++varIndexCounter[LONG_TYPE] == size) {
			checkIndexRange(size);
			longVarTable = Arrays.copyOf(longVarTable, size * 2);
		}
		return varIndexCounter[LONG_TYPE];
	}

	private static int reserveDoubleVarTable() {
		final int size = doubleVarTable.length;
		if(++varIndexCounter[DOUBLE_TYPE] == size) {
			checkIndexRange(size);
			doubleVarTable = Arrays.copyOf(doubleVarTable, size * 2);
		}
		return varIndexCounter[DOUBLE_TYPE];
	}

	private static int reserveBooleanVarTable() {
		final int size =booleanVarTable.length;
		if(++varIndexCounter[BOOLEAN_TYPE] == size) {
			checkIndexRange(size);
			booleanVarTable = Arrays.copyOf(booleanVarTable, size * 2);
		}
		return varIndexCounter[BOOLEAN_TYPE];
	}

	private static int reserveObjectVarTable() {
		final int size = objectVarTable.length;
		if(++varIndexCounter[OBJECT_TYPE] == size) {
			checkIndexRange(size);
			objectVarTable = Arrays.copyOf(objectVarTable, size * 2);
		}
		return varIndexCounter[OBJECT_TYPE];
	}

	private static void checkIndexRange(int size) {
		if(size >= MAX_INDEX) {
			throw new RuntimeException("too many global variable");
		}
	}

	public static void setLongVariable(int index, long value) {
		longVarTable[index] = value;
	}

	public static long getLongVariable(int index) {
		return longVarTable[index];
	}

	// for double variable
	public static void setDoubleVariable(int index, double value) {
		doubleVarTable[index] = value;
	}

	public static double getDoubleVariable(int index) {
		return doubleVarTable[index];
	}

	// for boolean variable
	public static void setBooleanVariable(int index, boolean value) {
		booleanVarTable[index] = value;
	}

	public static boolean getBooleanVariable(int index) {
		return booleanVarTable[index];
	}

	// for object variable
	public static void setObjectVariable(int index, Object value) {
		objectVarTable[index] = value;
	}

	public static Object getObjectVariable(int index) {
		return objectVarTable[index];
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
}
