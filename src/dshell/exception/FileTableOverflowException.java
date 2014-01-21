package dshell.exception;


public class FileTableOverflowException extends RelatedSyscallException {
	private static final long serialVersionUID = 1L;

	public FileTableOverflowException(String message, String commandName, String[] syscalls) {
		super(message, commandName, syscalls);
	}
}