package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENETUNREACH)
public class UnreachableNetworkException extends RelatedSyscallException {
	private static final long serialVersionUID = 7582354315727320975L;

	public UnreachableNetworkException(String message) {
		super(message);
	}
}