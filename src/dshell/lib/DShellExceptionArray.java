package dshell.lib;

import dshell.exception.DShellException;
import zen.deps.ZenObject;
import zen.deps.ZenObjectArray;

public class DShellExceptionArray extends ZenObject {
	private int size;
	private DShellException[] values;
	public DShellExceptionArray(int TypeId) {
		super(TypeId);
		this.values = new DShellException[1];
		this.size = 0;
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
		ZenObjectArray.ThrowOutOfArrayIndex(array.size, index);
		return null;
	}

	public final static void SetIndex(DShellExceptionArray array, long index, DShellException value) {
		if(index < array.size) {
			array.values[(int)index] = value;
		}
		ZenObjectArray.ThrowOutOfArrayIndex(array.size, index);
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
