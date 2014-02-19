package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EEXIST)
public class FileExistException extends RelatedSyscallException {
	private static final long serialVersionUID = -1147889522086285307L;

	public FileExistException(String message) {
		super(message);
	}
}