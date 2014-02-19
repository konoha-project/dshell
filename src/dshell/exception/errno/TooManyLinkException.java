package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EMLINK)
public class TooManyLinkException extends RelatedSyscallException {
	private static final long serialVersionUID = 8752586840100868249L;

	public TooManyLinkException(String message) {
		super(message);
	}
}
