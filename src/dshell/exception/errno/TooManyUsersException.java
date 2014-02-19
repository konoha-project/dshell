package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EUSERS)
public class TooManyUsersException extends RelatedSyscallException {
	private static final long serialVersionUID = -3444647688137490451L;

	public TooManyUsersException(String message) {
		super(message);
	}
}