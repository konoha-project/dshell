package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EHOSTUNREACH)
public class UnreachableHostException extends RelatedSyscallException {
	private static final long serialVersionUID = 1L;

	public UnreachableHostException(String message, String commandName, String[] syscalls) {
		super(message, commandName, syscalls);
	}
}