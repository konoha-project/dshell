package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EROFS)
public class ReadOnlyException extends RelatedSyscallException {
	private static final long serialVersionUID = 8332949929528954627L;

	public ReadOnlyException(String message) {
		super(message);
	}
}