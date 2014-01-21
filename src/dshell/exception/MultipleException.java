package dshell.exception;

import zen.deps.ZenArray;

public class MultipleException extends DShellException {
	private static final long serialVersionUID = 1L;
	private ZenArray<DShellException> exceptionArray;

	public MultipleException(String message, Exception[] exceptions) {
		super(message);
	}

	public ZenArray<DShellException> getExceptions() {
		return this.exceptionArray;
	}
}
