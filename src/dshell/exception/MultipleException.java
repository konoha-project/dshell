package dshell.exception;

import zen.deps.NativeTypeTable;
import zen.deps.ZenArray;

public class MultipleException extends DShellException {
	private static final long serialVersionUID = 1L;
	private ZenArray<DShellException> exceptionArray;

	public MultipleException(String message, Exception[] exceptions) {
		super(message);
		this.exceptionArray = ZenArray.NewZenArray(NativeTypeTable.GetZenType(DShellException.class));
	}

	public ZenArray<DShellException> getExceptions() {
		return this.exceptionArray;
	}
}
