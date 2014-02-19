package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EFBIG)
public class TooLargeFileException extends RelatedSyscallException {
	private static final long serialVersionUID = -7966116738015921365L;

	public TooLargeFileException(String message) {
		super(message);
	}
}