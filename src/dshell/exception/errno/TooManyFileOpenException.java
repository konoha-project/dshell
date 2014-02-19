package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EMFILE)
public class TooManyFileOpenException extends RelatedSyscallException {
	private static final long serialVersionUID = 9282872870301020L;

	public TooManyFileOpenException(String message) {
		super(message);
	}
}