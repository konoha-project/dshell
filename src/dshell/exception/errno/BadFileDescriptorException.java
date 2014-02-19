package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EBADF)
public class BadFileDescriptorException extends RelatedSyscallException {
	private static final long serialVersionUID = -7496151185611802517L;

	public BadFileDescriptorException(String message) {
		super(message);
	}
}