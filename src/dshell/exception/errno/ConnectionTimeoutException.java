package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ETIMEDOUT)
public class ConnectionTimeoutException extends RelatedSyscallException {
	private static final long serialVersionUID = -783718463247817643L;

	public ConnectionTimeoutException(String message) {
		super(message);
	}
}