package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.EPERM)
public class NotPermittedOperateException extends RelatedSyscallException {
	private static final long serialVersionUID = 2627959641734763573L;

	public NotPermittedOperateException(String message) {
		super(message);
	}
}