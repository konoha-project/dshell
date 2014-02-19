package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EPIPE)
public class BrokenPipeException extends RelatedSyscallException {
	private static final long serialVersionUID = -5163673987337012958L;

	public BrokenPipeException(String message) {
		super(message);
	}
}