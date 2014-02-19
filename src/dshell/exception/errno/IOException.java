package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EIO)
public class IOException extends RelatedSyscallException {
	private static final long serialVersionUID = -2925711332327816255L;

	public IOException(String message) {
		super(message);
	}
}