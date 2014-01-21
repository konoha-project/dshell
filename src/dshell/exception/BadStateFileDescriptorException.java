package dshell.exception;


public class BadStateFileDescriptorException extends RelatedSyscallException {
	private static final long serialVersionUID = 1L;

	public BadStateFileDescriptorException(String message, String commandName, String[] syscalls) {
		super(message, commandName, syscalls);
	}
}