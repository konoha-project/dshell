package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EDEADLK)
public class DeadLockException extends RelatedSyscallException {
	private static final long serialVersionUID = 1L;

	public DeadLockException(String message, String commandName, String[] syscalls) {
		super(message, commandName, syscalls);
	}
}