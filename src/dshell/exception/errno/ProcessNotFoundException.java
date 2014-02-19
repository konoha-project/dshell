package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ESRCH)
public class ProcessNotFoundException extends RelatedSyscallException {
	private static final long serialVersionUID = -3992933727935155963L;

	public ProcessNotFoundException(String message) {
		super(message);
	}
}
