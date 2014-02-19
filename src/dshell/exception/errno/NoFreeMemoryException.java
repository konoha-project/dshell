package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENOMEM)
public class NoFreeMemoryException extends RelatedSyscallException {
	private static final long serialVersionUID = -195959244637562160L;

	public NoFreeMemoryException(String message) {
		super(message);
	}
}