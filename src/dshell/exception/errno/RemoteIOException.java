package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EREMOTEIO)
public class RemoteIOException extends RelatedSyscallException {
	private static final long serialVersionUID = -539582706711746246L;

	public RemoteIOException(String message) {
		super(message);
	}
}