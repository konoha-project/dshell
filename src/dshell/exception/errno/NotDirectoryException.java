package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENOTDIR)
public class NotDirectoryException extends RelatedSyscallException {
	private static final long serialVersionUID = -8787884746109654469L;

	public NotDirectoryException(String message) {
		super(message);
	}
}