package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENOEXEC)
public class ExecutionFormatException extends RelatedSyscallException {
	private static final long serialVersionUID = 6039887131382912453L;

	public ExecutionFormatException(String message) {
		super(message);
	}
}
