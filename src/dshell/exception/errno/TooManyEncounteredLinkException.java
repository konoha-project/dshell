package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ELOOP)
public class TooManyEncounteredLinkException extends RelatedSyscallException {
	private static final long serialVersionUID = -5707416057493927952L;

	public TooManyEncounteredLinkException(String message) {
		super(message);
	}
}