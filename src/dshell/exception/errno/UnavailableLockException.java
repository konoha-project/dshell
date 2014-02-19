package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENOLCK)
public class UnavailableLockException extends RelatedSyscallException {
	private static final long serialVersionUID = 8589971637706803734L;

	public UnavailableLockException(String message) {
		super(message);
	}
}
