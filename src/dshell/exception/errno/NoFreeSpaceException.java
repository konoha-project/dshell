package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENOSPC)
public class NoFreeSpaceException extends RelatedSyscallException {
	private static final long serialVersionUID = 8768541609625410569L;

	public NoFreeSpaceException(String message) {
		super(message);
	}
}