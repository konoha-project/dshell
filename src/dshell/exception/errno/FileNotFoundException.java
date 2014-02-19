package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENOENT)
public class FileNotFoundException extends RelatedSyscallException {
	private static final long serialVersionUID = 6142467605943464028L;

	public FileNotFoundException(String message) {
		super(message);
	}
}