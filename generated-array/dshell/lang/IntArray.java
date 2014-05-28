// auto generated source file. do not edit me.
package dshell.lang;

import java.util.Arrays;

import dshell.internal.lib.Utils;
import dshell.lang.annotation.ArrayOp;
import dshell.lang.annotation.ArrayOp.ArrayOpType;
import dshell.lang.annotation.Shared;

/**
 * Int array for class Type value.
 * Can use it as a stack.
 * @author skgchxngsxyz-osx
 *
 */
public class IntArray implements Cloneable {
	private final static int defaultArraySize = 16;

	/**
	 * contains array elements.
	 */
	private long[] values;

	/**
	 * represents currently containing element size.
	 */
	private int size;

	public IntArray(long[] values) {
		this.size = values.length;
		this.values = new long[this.size < defaultArraySize ? defaultArraySize : this.size];
		System.arraycopy(values, 0, this.values, 0, this.size);
	}

	/**
	 * called from clone()
	 */
	private IntArray() {
	}

	private void throwIfIndexOutOfRange(long index) {
		if(index < 0 || index >= this.size()) {
			throw new OutOfIndexException("array size is " + this.size() + ", but index is " + index);
		}
	}

	private void expandIfNoFreeSpace() {
		if(this.size() == this.values.length) {
			Arrays.copyOf(this.values, this.values.length * 2);
		}
	}

	@Shared
	public long size() {
		return this.size;
	}

	@Shared
	public boolean isEmpty() {
		return this.size() == 0;
	}

	@Shared
	public void clear() {
		this.size = 0;
	}

	@Shared
	@ArrayOp(ArrayOpType.Getter)
	
	public long get(long index) {
		this.throwIfIndexOutOfRange(index);
		return this.values[(int) index];
	}

	@Shared
	@ArrayOp(ArrayOpType.Setter)
	public void set(long index,  long value) {
		this.throwIfIndexOutOfRange(index);
		this.values[(int) index] = value;
	}

	@Shared
	public void add( long value) {
		this.expandIfNoFreeSpace();
		this.values[this.size++] = value;
	}

	@Shared
	public void insert(long index,  long value) {
		if(index == this.size()) {
			this.add(value);
			return;
		}
		this.throwIfIndexOutOfRange(index);
		this.expandIfNoFreeSpace();
		long[] newValues = new long[this.values.length];
		final int i = (int) index;
		System.arraycopy(this.values, 0, newValues, 0, i);
		newValues[i] = value;
		System.arraycopy(this.values, i, newValues, i + 1, this.size - i);
		this.values = newValues;
		this.size++;
	}

	@Shared
	
	public long remove(long index) {
		long value = this.get(index);
		int i = (int) index;
		System.arraycopy(this.values, i + 1, this.values, i, this.size - 1);
		this.size--;
		return value;
	}

	@Shared
	public void push(long value) {
		this.add(value);
	}

	@Shared
	
	public long pop() {
		return this.values[--this.size];
	}

	@Shared
	public IntArray clone() {
		if(this.isEmpty()) {
			return new IntArray(new long[]{});
		}
		long[] newValues = new long[this.size];
		IntArray clonedArray = new IntArray();
		clonedArray.size = this.size;
		System.arraycopy(this.values, 0, newValues, 0, this.size);
		clonedArray.values = newValues;
		return clonedArray;
	}

	@Override
	public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("[");
		for(int i = 0; i < this.size; i++) {
			if(i > 0) {
				sBuilder.append(", ");
			}
			Utils.appendStringifiedValue(sBuilder, this.values[i]);
		}
		sBuilder.append("]");
		return sBuilder.toString();
	}
}
