package dshell.exception;

import dshell.lib.DShellExceptionArray;
import zen.codegen.jvm.JavaTypeTable;
import zen.type.ZType;

public class MultipleException extends DShellException {
	private static final long serialVersionUID = 1L;
	private DShellExceptionArray exceptionArray;

	public MultipleException(String message, DShellException[] exceptions) {
		super(message);
		ZType nativeType = JavaTypeTable.GetZenType(DShellException.class);
		this.exceptionArray = new DShellExceptionArray(nativeType.TypeId);
		for(DShellException exception : exceptions) {
			this.exceptionArray.Add(exception);
		}
	}

	public DShellExceptionArray getExceptions() {
		return this.exceptionArray;
	}

	@Override
	public String toString() {
		return this.getClass().getCanonicalName() + ": " + this.exceptionArray.toString();
	}
}
