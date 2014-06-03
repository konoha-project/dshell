package dshell.lang;

import dshell.annotation.Shared;
import dshell.annotation.SharedClass;

@SharedClass("Exception")
public class KeyNotFoundException extends dshell.lang.Exception {
	private static final long serialVersionUID = -5910680153988070736L;

	@Shared
	public KeyNotFoundException() {
		super();
	}

	@Shared
	public KeyNotFoundException(String message) {
		super(message);
	}
}
