package dshell.exception;

import dshell.lib.DShellExceptionArray;
import zen.codegen.jvm.JavaTypeTable;
import zen.type.ZType;

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
			ZType nativeType = JavaTypeTable.GetZenType(DShellException.class);
			this.exceptionArray = new DShellExceptionArray(nativeType.TypeId, this.exceptions);
		}
		return this.exceptionArray;
	}

	@Override
	public String toString() {
		return this.getClass().getCanonicalName() + ": " + this.getExceptions().toString();
	}
}
