package dshell.exception;


public class RemoteIOException extends RelatedSyscallException {
	private static final long serialVersionUID = 1L;

	public RemoteIOException(String message, String commandName, String[] syscalls) {
		super(message, commandName, syscalls);
	}
}