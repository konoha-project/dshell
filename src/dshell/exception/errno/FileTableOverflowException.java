package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENFILE)
public class FileTableOverflowException extends RelatedSyscallException {
	private static final long serialVersionUID = 3318452069127891085L;

	public FileTableOverflowException(String message) {
		super(message);
	}
}