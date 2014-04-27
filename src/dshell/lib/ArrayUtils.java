package dshell.lib;

import libbun.encode.jvm.JavaTypeTable;
import dshell.exception.DShellException;
import libbun.type.BGenericType;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.util.BStringArray;
import libbun.util.ZObjectArray;

public class ArrayUtils {
	public static DShellExceptionArray createExceptionArray(DShellException[] exceptions) {
		BType exceptionType = JavaTypeTable.GetBunType(DShellException.class);
		BType exceptionArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, exceptionType);
		return new DShellExceptionArray(exceptionArrayType.TypeId, exceptions);
	}

	public static class DShellExceptionArray extends ZObjectArray {
		private DShellExceptionArray(int TypeId, Object[] Values) {
			super(TypeId, Values);
		}

		@Override
		protected void Stringfy(StringBuilder sb) {
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
		BType taskType = JavaTypeTable.GetBunType(Task.class);
		BType taskArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, taskType);
		return new TaskArray(taskArrayType.TypeId, tasks);
	}

	public static class TaskArray extends ZObjectArray {
		private TaskArray(int TypeId, Object[] Values) {
			super(TypeId, Values);
		}

		@Override
		protected void Stringfy(StringBuilder sb) {
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

	public static BStringArray createStringArray(String[] values) {
		BType stringArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.StringType);
		return new BStringArray(stringArrayType.TypeId, values);
	}
}
