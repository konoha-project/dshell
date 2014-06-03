package dshell.lang;

import dshell.annotation.ArrayOp;
import dshell.annotation.Shared;
import dshell.annotation.Wrapper;
import dshell.annotation.ArrayOp.ArrayOpType;

/**
 * String api for D-Shell.
 * @author skgchxngsxyz-osx
 *
 */
public final class StringUtils {
	private static void throwIfIndexOutOfRange(String value, long index) {
		throwIfIndexOutOfRange(value, index, true);
	}

	private static void throwIfIndexOutOfRange(String value, long index, boolean includeSize) {
		int size = value.length();
		if(index < 0 || index > size || (includeSize && index == size)) {
			throw new OutOfIndexException("string size is " + size + ", but index is " + index);
		}
	}

	// String API declaration
	@Shared @Wrapper
	public static long size(String recv) {
		return recv.length();
	}

	@Shared @Wrapper
	public static String substring(String recv, long startIndex) {
		throwIfIndexOutOfRange(recv, startIndex);
		return recv.substring((int) startIndex);
	}

	@Shared @Wrapper
	public static String substring(String recv, long startIndex, long endIndex) {
		throwIfIndexOutOfRange(recv, startIndex);
		throwIfIndexOutOfRange(recv, endIndex, false);
		if(startIndex > endIndex) {
			throw new OutOfIndexException("start index = " + startIndex + ", end index = " + endIndex);
		}
		return recv.substring((int) startIndex, (int) endIndex);
	}

	@Shared @Wrapper
	public static boolean equals(String recv, String target) {
		if(target == null) {
			return false;
		}
		return recv.equals(target);
	}

	@Shared @Wrapper
	public static boolean startsWith(String recv, String target) {
		if(target == null) {
			return false;
		}
		return recv.startsWith(target);
	}

	@Shared @Wrapper
	public static boolean endsWith(String recv, String target) {
		if(target == null) {
			return false;
		}
		return recv.endsWith(target);
	}

	@ArrayOp(ArrayOpType.Getter)
	@Shared @Wrapper
	public static String get(String recv, long index) {
		throwIfIndexOutOfRange(recv, index);
		return Character.toString(recv.charAt((int) index));
	}

	@Shared @Wrapper
	public static String clone(String recv) {
		return recv.substring(0, recv.length());
	}
}
