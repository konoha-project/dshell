package dshell.exception.errno;

import dshell.exception.RelatedSyscallException;
import dshell.lib.DerivedFromErrno;
import dshell.lib.Errno;

@DerivedFromErrno(value = Errno.ENODEV)
public class DeviceNotFoundException extends RelatedSyscallException {
	private static final long serialVersionUID = -2089536947016513934L;

	public DeviceNotFoundException(String message) {
		super(message);
	}
}