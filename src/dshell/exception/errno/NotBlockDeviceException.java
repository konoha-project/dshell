package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENOTBLK)
public class NotBlockDeviceException extends RelatedSyscallException {
	private static final long serialVersionUID = -1664763329262707450L;

	public NotBlockDeviceException(String message) {
		super(message);
	}
}
