package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EISDIR)
public class IsDirectoryException extends RelatedSyscallException {
	private static final long serialVersionUID = 1L;

	public IsDirectoryException(String message, String commandName, String[] syscalls) {
		super(message, commandName, syscalls);
	}
}