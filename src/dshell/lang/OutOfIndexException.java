package dshell.lang;

import dshell.annotation.Shared;
import dshell.annotation.SharedClass;

@SharedClass("Exception")
public class OutOfIndexException extends dshell.lang.Exception {
	private static final long serialVersionUID = 5229764115171847285L;

	@Shared
	public OutOfIndexException() {
		super();
	}

	@Shared
	public OutOfIndexException(String message) {
		super(message);
	}
}
