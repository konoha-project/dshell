package dshell.exception;

import dshell.lib.DefinedArray.DShellExceptionArray;
import zen.codegen.jvm.JavaTypeTable;
import zen.type.ZGenericType;
import zen.type.ZType;
import zen.type.ZTypePool;

public class MultipleException extends DShellException {
	private static final long serialVersionUID = 164898266354483402L;
	private DShellException[] exceptions;
	transient private DShellExceptionArray exceptionArray;

	public MultipleException(String message, DShellException[] exceptions) {
		super(message);
		int size = exceptions.length;
		this.exceptions = new DShellException[size];
		for(int i = 0; i < size; i++) {
			this.exceptions[i] = exceptions[i];
		}
	}

	public DShellExceptionArray getExceptions() {
		if(exceptionArray == null) {
			ZType exceptionType = JavaTypeTable.GetZenType(DShellException.class);
			ZType exceptionArrayType = ZTypePool._GetGenericType1(ZGenericType._ArrayType, exceptionType);
			this.exceptionArray = new DShellExceptionArray(exceptionArrayType.TypeId, this.exceptions);
		}
		return this.exceptionArray;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": " + this.getExceptions().toString();
	}
}
