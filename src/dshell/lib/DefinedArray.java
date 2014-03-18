package dshell.lib;

import dshell.exception.DShellException;
import zen.codegen.jvm.JavaTypeTable;
import zen.type.ZGenericType;
import zen.type.ZType;
import zen.type.ZTypePool;
import zen.util.ZObjectArray;

public class DefinedArray {
	public static DShellExceptionArray createExceptionArray(DShellException[] exceptions) {
		ZType exceptionType = JavaTypeTable.GetZenType(DShellException.class);
		ZType exceptionArrayType = ZTypePool._GetGenericType1(ZGenericType._ArrayType, exceptionType);
		return new DShellExceptionArray(exceptionArrayType.TypeId, exceptions);
	}

	public static class DShellExceptionArray extends ZObjectArray {
		private DShellExceptionArray(int TypeId, Object[] Values) {
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

	public static TaskArray createTaskArray(Task[] tasks) {
		ZType taskType = JavaTypeTable.GetZenType(Task.class);
		ZType taskArrayType = ZTypePool._GetGenericType1(ZGenericType._ArrayType, taskType);
		return new TaskArray(taskArrayType.TypeId, tasks);
	}

	public static class TaskArray extends ZObjectArray {
		private TaskArray(int TypeId, Object[] Values) {
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
