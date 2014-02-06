package dshell.exception;

public class DShellException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private String errorMessage;

	public DShellException(String message) {
		super(message);
		this.errorMessage = "";
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}
}