package dshell.lib;

import dshell.exception.DShellException;
import zen.util.ZObjectArray;

public class DefinedArray {
	public static class DShellExceptionArray extends ZObjectArray {
		public DShellExceptionArray(int TypeId, Object[] Values) {
			super(TypeId, Values);
		}

		@Override protected void Stringfy(StringBuilder sb) {
			sb.append("[");
			for(int i = 0; i < this.Size(); i++) {
				if(i > 0) {
					sb.append(", ");
				}
				this.AppendStringBuffer(sb, ZObjectArray.GetIndex(this, i).toString());
			}
			sb.append("]");
		}

		public final static DShellException GetIndex(DShellExceptionArray a, long Index) {
			return (DShellException) ZObjectArray.GetIndex(a, Index);
		}
	}

	public static class TaskArray extends ZObjectArray {
		public TaskArray(int TypeId, Object[] Values) {
			super(TypeId, Values);
		}

		@Override protected void Stringfy(StringBuilder sb) {
			sb.append("[");
			for(int i = 0; i < this.Size(); i++) {
				if(i > 0) {
					sb.append(", ");
				}
				this.AppendStringBuffer(sb, ZObjectArray.GetIndex(this, i).toString());
			}
			sb.append("]");
		}

		public final static Task GetIndex(TaskArray a, long Index) {
			return (Task) ZObjectArray.GetIndex(a, Index);
		}
	}
}
