package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENOBUFS)
public class NoBufferSpaceException extends RelatedSyscallException {
	private static final long serialVersionUID = -5330482173311853395L;

	public NoBufferSpaceException(String message) {
		super(message);
	}
}