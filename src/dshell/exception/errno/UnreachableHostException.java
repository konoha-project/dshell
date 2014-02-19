package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EHOSTUNREACH)
public class UnreachableHostException extends RelatedSyscallException {
	private static final long serialVersionUID = 1597304418977336685L;

	public UnreachableHostException(String message) {
		super(message);
	}
}