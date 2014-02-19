package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ESPIPE)
public class IllegalSeekException extends RelatedSyscallException {
	private static final long serialVersionUID = -5170190658505999982L;

	public IllegalSeekException(String message) {
		super(message);
	}
}