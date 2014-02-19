package dshell.exception;

public class UnimplementedErrnoException extends RelatedSyscallException {
	private static final long serialVersionUID = 3957045480645559921L;

	public UnimplementedErrnoException(String message) {
		super(message);
	}
	
	@Override
	public String toString() {
		return super.toString() + " :" + this.getErrno() + " has not supported yet";
	}
}
