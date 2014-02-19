package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EFAULT)
public class BadAddressException extends RelatedSyscallException {
	private static final long serialVersionUID = 5757398124745842354L;

	public BadAddressException(String message) {
		super(message);
	}
}
