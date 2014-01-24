package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ETXTBSY)
public class BusyTextFileException extends RelatedSyscallException {
	private static final long serialVersionUID = 1L;

	public BusyTextFileException(String message, String commandName, String[] syscalls) {
		super(message, commandName, syscalls);
	}
}
