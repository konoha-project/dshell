package dshell.exception;


public class UnreachableHostException extends RelatedSyscallException {
	private static final long serialVersionUID = 1L;

	public UnreachableHostException(String message, String commandName, String[] syscalls) {
		super(message, commandName, syscalls);
	}
}