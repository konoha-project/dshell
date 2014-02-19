package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EACCES)
public class NotPermittedException extends RelatedSyscallException {
	private static final long serialVersionUID = 1796678881928391383L;

	public NotPermittedException(String message) {
		super(message);
	}
}