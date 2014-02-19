package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EAGAIN)
public class TemporaryUnavailableException extends RelatedSyscallException {
	private static final long serialVersionUID = -260611456289777692L;

	public TemporaryUnavailableException(String message) {
		super(message);
	}
}