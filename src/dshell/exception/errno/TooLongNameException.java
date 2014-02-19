package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENAMETOOLONG)
public class TooLongNameException extends RelatedSyscallException {
	private static final long serialVersionUID = 2424365446992735197L;

	public TooLongNameException(String message) {
		super(message);
	}
}