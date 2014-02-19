package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EBUSY)
public class BusyResourceException extends RelatedSyscallException {
	private static final long serialVersionUID = -398174827082926739L;

	public BusyResourceException(String message) {
		super(message);
	}
}