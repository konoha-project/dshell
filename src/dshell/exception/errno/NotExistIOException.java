package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENXIO)
public class NotExistIOException extends RelatedSyscallException {
	private static final long serialVersionUID = 1645567429022447252L;

	public NotExistIOException(String message) {
		super(message);
	}
}
