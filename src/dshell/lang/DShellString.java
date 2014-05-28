package dshell.lang;

import dshell.lang.annotation.ArrayOp;
import dshell.lang.annotation.Shared;
import dshell.lang.annotation.ArrayOp.ArrayOpType;

/**
 * String class for D-Shell.
 * It contains java.lang.String object.
 * @author skgchxngsxyz-osx
 *
 */
public final class DShellString {
	/**
	 * Java native string.
	 */
	private final String value;

	private final long size;

	private DShellString(String value) {
		this.value = value;
		this.size = this.value.length();
	}

	/**
	 * Called from code generator.
	 * @param value
	 * @return
	 */
	public static DShellString newString(String value) {
		return new DShellString(value);
	}

	private void throwIfIndexOutOfRange(long index) {
		this.throwIfIndexOutOfRange(index, true);
	}

	private void throwIfIndexOutOfRange(long index, boolean includeSize) {
		if(index < 0 || index > this.size() || (includeSize && index == this.size())) {
			throw new OutOfIndexException("string size is " + this.size() + ", but index is " + index);
		}
	}

	// method declaration
	@Shared
	public long size() {
		return this.size;
	}

	@Shared
	public DShellString substring(long startIndex) {
		this.throwIfIndexOutOfRange(startIndex);
		return new DShellString(this.value.substring((int) startIndex));
	}

	@Shared
	public DShellString substring(long startIndex, long endIndex) {
		this.throwIfIndexOutOfRange(startIndex);
		this.throwIfIndexOutOfRange(endIndex, false);
		if(startIndex > endIndex) {
			throw new OutOfIndexException("start index = " + startIndex + ", end index = " + endIndex);
		}
		return new DShellString(this.value.substring((int) startIndex, (int) endIndex));
	}

	@Shared
	public boolean equals(DShellString target) {
		if(target == null) {
			return false;
		}
		return this.value.equals(target.value);
	}

	@Shared
	public boolean startsWith(DShellString target) {
		if(target == null) {
			return false;
		}
		return this.value.startsWith(target.value);
	}

	@Shared
	public boolean endsWith(DShellString target) {
		if(target == null) {
			return false;
		}
		return this.value.endsWith(target.value);
	}

	@ArrayOp(ArrayOpType.Getter)
	@Shared
	public DShellString get(long index) {
		this.throwIfIndexOutOfRange(index);
		return new DShellString(Character.toString(this.value.charAt((int) index)));
	}

	@Override
	public String toString() {
		return this.value;
	}

	@Shared
	public DShellString clone() {
		return this.substring(0, this.size);
	}
}
