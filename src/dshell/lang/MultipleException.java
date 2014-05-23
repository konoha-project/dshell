package dshell.lang;

import libbun.util.BArray;

public class MultipleException extends DShellException {
	private static final long serialVersionUID = 164898266354483402L;
	private DShellException[] exceptions;
	transient private BArray<DShellException> exceptionArray;

	public MultipleException(String message, DShellException[] exceptions) {
		super(message);
		int size = exceptions.length;
		this.exceptions = new DShellException[size];
		for(int i = 0; i < size; i++) {
			this.exceptions[i] = exceptions[i];
		}
	}

	public BArray<DShellException> getExceptions() {
		if(this.exceptionArray == null) {
			this.exceptionArray = new BArray<DShellException>(0, exceptions);
		}
		return this.exceptionArray;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": " + this.getExceptions().toString();
	}
}
