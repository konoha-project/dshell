package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ERANGE)
public class UnrepresentableResultException extends RelatedSyscallException {
	private static final long serialVersionUID = 8753352643952859835L;

	public UnrepresentableResultException(String message) {
		super(message);
	}
}
