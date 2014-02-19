package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EDOM)
public class ArgumentOutOfDomainException extends RelatedSyscallException {
	private static final long serialVersionUID = -2231772718959423178L;

	public ArgumentOutOfDomainException(String message) {
		super(message);
	}
}
