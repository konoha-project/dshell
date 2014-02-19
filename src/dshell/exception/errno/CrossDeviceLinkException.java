package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EXDEV)
public class CrossDeviceLinkException extends RelatedSyscallException {
	private static final long serialVersionUID = -2542368498065138082L;

	public CrossDeviceLinkException(String message) {
		super(message);
	}
}
