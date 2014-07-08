package dshell.lang;

import java.util.Arrays;

import dshell.annotation.ArrayOp;
import dshell.annotation.GenericClass;
import dshell.annotation.Shared;
import dshell.annotation.SharedClass;
import dshell.annotation.TypeAlias;
import dshell.annotation.ArrayOp.ArrayOpType;
import dshell.internal.lib.Utils;

/**
 * Generic array for class Type value.
 * Can use it as a stack.
 * @author skgchxngsxyz-osx
 *
 */
@SharedClass
@GenericClass(values = {"@T"})
public class GenericArray implements Cloneable {
	private final static int defaultArraySize = 16;

	/**
	 * contains array elements.
	 */
	private Object[] values;

	/**
	 * represents currently containing element size.
	 */
	private int size;

	public GenericArray(Object[] values) {
		this.size = values.length;
		this.values = new Object[this.size < defaultArraySize ? defaultArraySize : this.size];
		System.arraycopy(values, 0, this.values, 0, this.size);
	}

	@Shared
	public GenericArray() {
		this(new Object[]{});
	}

	/**
	 * called from clone
	 * @param enableAlloc
	 * - meaningless parameter.
	 */
	private GenericArray(boolean enableAlloc) {
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
		if(this.size > defaultArraySize) {
			this.values = new Object[defaultArraySize];
		}
		this.size = 0;
	}

	@Shared
	@ArrayOp(ArrayOpType.Getter)
	@TypeAlias("@T")
	public Object get(long index) {
		this.throwIfIndexOutOfRange(index);
		return this.values[(int) index];
	}

	@Shared
	@ArrayOp(ArrayOpType.Setter)
	public void set(
			long index, 
			@TypeAlias("@T")
			Object value) {
		this.throwIfIndexOutOfRange(index);
		this.values[(int) index] = value;
	}

	@Shared
	public void add(
			@TypeAlias("@T")
			Object value) {
		this.expandIfNoFreeSpace();
		this.values[this.size++] = value;
	}

	@Shared
	public void insert(
			long index, 
			@TypeAlias("@T")
			Object value) {
		if(index == this.size()) {
			this.add(value);
			return;
		}
		this.throwIfIndexOutOfRange(index);
		this.expandIfNoFreeSpace();
		Object[] newValues = new Object[this.values.length];
		final int i = (int) index;
		System.arraycopy(this.values, 0, newValues, 0, i);
		newValues[i] = value;
		System.arraycopy(this.values, i, newValues, i + 1, this.size - i);
		this.values = newValues;
		this.size++;
	}

	@Shared
	@TypeAlias("@T")
	public Object remove(long index) {
		Object value = this.get(index);
		int i = (int) index;
		System.arraycopy(this.values, i + 1, this.values, i, this.size - 1);
		this.size--;
		return value;
	}

	@Shared
	public void push(Object value) {
		this.add(value);
	}

	@Shared
	@TypeAlias("@T")
	public Object pop() {
		if(this.size == 0) {
			throw new OutOfIndexException("current index is 0");
		}
		return this.values[--this.size];
	}

	@Shared
	public void trim() {
		if(this.size > 0) {
			Arrays.copyOf(this.values, this.size);
		}
	}

	//@Shared
	//@TypeAlias("Array<@T>")
	public GenericArray clone() {
		if(this.isEmpty()) {
			return new GenericArray(new Object[]{});
		}
		Object[] newValues = new Object[this.size];
		GenericArray clonedArray = new GenericArray(false);
		clonedArray.size = this.size;
		System.arraycopy(this.values, 0, newValues, 0, this.size);
		clonedArray.values = newValues;
		return clonedArray;
	}

	@Shared
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
