package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ECONNREFUSED)
public class ConnectionRefusedException extends RelatedSyscallException {
	private static final long serialVersionUID = -5631944596016935849L;

	public ConnectionRefusedException(String message) {
		super(message);
	}
}