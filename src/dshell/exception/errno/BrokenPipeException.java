package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EPIPE)
public class BrokenPipeException extends RelatedSyscallException {
	private static final long serialVersionUID = 1L;

	public BrokenPipeException(String message, String commandName, String[] syscalls) {
		super(message, commandName, syscalls);
	}
}