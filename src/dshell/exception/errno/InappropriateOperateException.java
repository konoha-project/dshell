package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENOTTY)
public class InappropriateOperateException extends RelatedSyscallException {
	private static final long serialVersionUID = -9048472347245941031L;

	public InappropriateOperateException(String message) {
		super(message);
	}
}