package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EBADFD)
public class BadStateFileDescriptorException extends RelatedSyscallException {
	private static final long serialVersionUID = 1323523835328835128L;

	public BadStateFileDescriptorException(String message) {
		super(message);
	}
}