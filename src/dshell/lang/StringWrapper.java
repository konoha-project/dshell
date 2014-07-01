package dshell.lang;

import dshell.annotation.ArrayOp;
import dshell.annotation.Shared;
import dshell.annotation.SharedClass;
import dshell.annotation.Wrapper;
import dshell.annotation.ArrayOp.ArrayOpType;
import dshell.annotation.WrapperClass;

/**
 * String api for D-Shell.
 * @author skgchxngsxyz-osx
 *
 */

@SharedClass
@WrapperClass
public final class StringWrapper {
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
	public static String sliceFrom(String recv, long startIndex) {
		throwIfIndexOutOfRange(recv, startIndex);
		return recv.substring((int) startIndex);
	}

	@Shared @Wrapper
	public static String sliceTo(String recv, long endIndex) {
		throwIfIndexOutOfRange(recv, endIndex);
		return recv.substring(0, (int) endIndex);
	}

	@Shared @Wrapper
	public static String slice(String recv, long startIndex, long endIndex) {
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

	@Shared @Wrapper
	public static String replace(String recv, String regex, String replace) {
		return recv.replaceAll(regex, replace);
	}

	@ArrayOp(ArrayOpType.Getter)
	@Shared @Wrapper
	public static String get(String recv, long index) {
		throwIfIndexOutOfRange(recv, index);
		return Character.toString(recv.charAt((int) index));
	}

	@Shared @Wrapper
	public static String trim(String recv) {
		return recv.trim();
	}

	@Shared @Wrapper
	public static long indexOf(String recv, String str) {
		return recv.indexOf(str);
	}

	@Shared @Wrapper
	public static long lastIndexOf(String recv, String str) {
		return recv.lastIndexOf(str);
	}

	@Shared @Wrapper
	public static String clone(String recv) {
		return recv.substring(0, recv.length());
	}
}
