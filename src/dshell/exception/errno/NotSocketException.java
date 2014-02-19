package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENOTSOCK)
public class NotSocketException extends RelatedSyscallException {
	private static final long serialVersionUID = -3934984088546370161L;

	public NotSocketException(String message) {
		super(message);
	}
}