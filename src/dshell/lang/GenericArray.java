package dshell.lang;

import java.util.Arrays;

import dshell.lang.annotation.ArrayOp;
import dshell.lang.annotation.ArrayOp.ArrayOpType;
import dshell.lang.annotation.Exportable;
import dshell.lang.annotation.GenericClass;
import dshell.lang.annotation.TypeParameter;

/**
 * Generic Array for class Type value.
 * @author skgchxngsxyz-osx
 *
 */
@GenericClass
public class GenericArray {
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
		this.values = new Object[defaultArraySize];
		this.size = values.length;
		System.arraycopy(values, 0, this.values, 0, this.size);
	}

	private static void throwIfIndexOutOfRange(long index, GenericArray array) {
		if(index < 0 || index >= array.size()) {
			throw new OutOfIndexException("array size is " + array.size + ", but index is " + index);
		}
	}

	@Exportable
	public long size() {
		return this.size;
	}

	@Exportable
	@ArrayOp(ArrayOpType.Getter)
	@TypeParameter()
	public Object get(long index) {
		throwIfIndexOutOfRange(index, this);
		return this.values[(int) index];
	}

	@Exportable
	@ArrayOp(ArrayOpType.Setter)
	public void set(long index, 
			@TypeParameter() Object value) {
		throwIfIndexOutOfRange(index, this);
		this.values[(int) index] = value;
	}

	@Exportable
	public void add(@TypeParameter() Object value) {
		int index = this.size;
		if(index == this.values.length) {
			Arrays.copyOf(this.values, this.values.length * 2);
		}
		this.values[index++] = value;
		this.size = index;
	}
}
