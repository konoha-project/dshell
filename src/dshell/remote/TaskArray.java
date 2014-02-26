package dshell.remote;

import java.io.Serializable;

import dshell.lib.Task;
import zen.deps.ZObject;
import zen.deps.ZObjectArray;

public class TaskArray extends ZObject implements Serializable {
	private static final long serialVersionUID = -64642177189609802L;
	private int size;
	private Task[] values;
	public TaskArray(int TypeId, Task[] values) {
		super(TypeId);
		this.values = values;
		this.size = this.values.length;
	}
	@Override protected void Stringfy(StringBuilder sb) {
		sb.append("[");
		int i = 0;
		while(i < this.size) {
			if(i > 0) {
				sb.append(", ");
			}
			sb.append(this.values[i].getClass().getSimpleName());
			i = i + 1;
		}
		sb.append("]");
	}

	public final long Size() {
		return this.size;
	}

	public final static Task GetIndex(TaskArray array, long index) {
		if(index < array.size) {
			return array.values[(int)index];
		}
		ZObjectArray.ThrowOutOfArrayIndex(array.size, index);
		return null;
	}

	public final static void SetIndex(TaskArray array, long index, Task value) {
		if(index < array.size) {
			array.values[(int)index] = value;
		}
		ZObjectArray.ThrowOutOfArrayIndex(array.size, index);
	}

	public final void Add(Task value) {
		if(this.size == this.values.length) {
			Task[] newValues = new Task[this.values.length*2];
			System.arraycopy(this.values, 0, newValues, 0, this.size);
			this.values = newValues;
		}
		this.values[this.size] = value;
		this.size = this.size + 1;
	}
}
