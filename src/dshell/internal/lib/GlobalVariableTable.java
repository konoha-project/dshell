package dshell.internal.lib;

import java.util.Arrays;

/**
 * contain global variable.
 * @author skgchxngsxyz-osx
 *
 */
public class GlobalVariableTable {
	private final static int MAX_INDEX = Integer.MAX_VALUE;
	private final static int defaultTableSize = 16;

	/**
	 * global variable table for long value.
	 */
	public static long[]    longVarTable;

	/**
	 * global variable table for double value.
	 */
	public static double[]  doubleVarTable;

	/**
	 * global variable table for boolean value.
	 */
	public static boolean[] booleanVarTable;

	/**
	 * global variable table for object value.
	 */
	public static Object[]  objectVarTable;

	private static int longVarIndexCount    = 0;
	private static int doubleVarIndexCount  = 0;
	private static int booleanVarIndexCount = 0;
	private static int objectVarIndexCount  = 0;

	static {
		longVarTable = new long[defaultTableSize];
		doubleVarTable = new double[defaultTableSize];
		booleanVarTable = new boolean[defaultTableSize];
		objectVarTable = new Object[defaultTableSize];
	}

	// reserve variable table
	public static int reserveLongVarTable() {
		final int size = longVarTable.length;
		if(longVarIndexCount == size) {
			checkIndexRange(size);
			longVarTable = Arrays.copyOf(longVarTable, size * 2);
		}
		return longVarIndexCount++;
	}

	public static int reserveDoubleVarTable() {
		final int size = doubleVarTable.length;
		if(doubleVarIndexCount == size) {
			checkIndexRange(size);
			doubleVarTable = Arrays.copyOf(doubleVarTable, size * 2);
		}
		return doubleVarIndexCount++;
	}

	public static int reserveBooleanVarTable() {
		final int size =booleanVarTable.length;
		if(booleanVarIndexCount == size) {
			checkIndexRange(size);
			booleanVarTable = Arrays.copyOf(booleanVarTable, size * 2);
		}
		return booleanVarIndexCount++;
	}

	public static int reserveObjectVarTable() {
		final int size = objectVarTable.length;
		if(objectVarIndexCount == size) {
			checkIndexRange(size);
			objectVarTable = Arrays.copyOf(objectVarTable, size * 2);
		}
		return objectVarIndexCount++;
	}

	private static void checkIndexRange(int size) {
		if(size >= MAX_INDEX) {
			throw new RuntimeException("too many global variable");
		}
	}
}
