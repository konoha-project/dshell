package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ECHILD)
public class NoChildException extends RelatedSyscallException {
	private static final long serialVersionUID = 7148181168710617339L;

	public NoChildException(String message) {
		super(message);
	}
}