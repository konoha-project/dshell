package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ECHILD)
public class NoChildException extends RelatedSyscallException {
	private static final long serialVersionUID = 1L;

	public NoChildException(String message, String commandName, String[] syscalls) {
		super(message, commandName, syscalls);
	}
}