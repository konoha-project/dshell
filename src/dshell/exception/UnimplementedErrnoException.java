package dshell.exception;

public class UnimplementedErrnoException extends RelatedSyscallException {
	private static final long serialVersionUID = 1L;

	public UnimplementedErrnoException(String message, String commandName, String[] syscalls) {
		super(message, commandName, syscalls);
	}
	
	@Override
	public String toString() {
		return super.toString() + " :" + this.getErrno() + " has not supported yet";
	}
}
