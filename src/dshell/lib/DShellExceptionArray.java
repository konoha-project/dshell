package dshell.lib;

import java.io.Serializable;

import dshell.exception.DShellException;
import zen.deps.ZObject;
import zen.deps.ZObjectArray;

public class DShellExceptionArray extends ZObject implements Serializable {
	private static final long serialVersionUID = -64642177189609802L;
	private int size;
	private DShellException[] values;
	public DShellExceptionArray(int TypeId, DShellException[] values) {
		super(TypeId);
		this.values = values;
		this.size = this.values.length;
	}
	@Override public String toString() {
		String s = "[";
		int i = 0;
		while(i < this.size) {
			if(i > 0) {
				s += ", ";
			}
			s += this.values[i].getClass().getSimpleName();
			i = i + 1;
		}
		return s + "]";
	}

	public final long Size() {
		return this.size;
	}

	public final static DShellException GetIndex(DShellExceptionArray array, long index) {
		if(index < array.size) {
			return array.values[(int)index];
		}
		ZObjectArray.ThrowOutOfArrayIndex(array.size, index);
		return null;
	}

	public final static void SetIndex(DShellExceptionArray array, long index, DShellException value) {
		if(index < array.size) {
			array.values[(int)index] = value;
		}
		ZObjectArray.ThrowOutOfArrayIndex(array.size, index);
	}

	public final void Add(DShellException value) {
		if(this.size == this.values.length) {
			DShellException[] newValues = new DShellException[this.values.length*2];
			System.arraycopy(this.values, 0, newValues, 0, this.size);
			this.values = newValues;
		}
		this.values[this.size] = value;
		this.size = this.size + 1;
	}
}
