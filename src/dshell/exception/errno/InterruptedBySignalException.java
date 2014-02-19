package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EINTR)
public class InterruptedBySignalException extends RelatedSyscallException {
	private static final long serialVersionUID = -2966050458509865009L;

	public InterruptedBySignalException(String message) {
		super(message);
	}
}