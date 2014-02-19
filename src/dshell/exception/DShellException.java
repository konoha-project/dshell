package dshell.exception;

public class DShellException extends RuntimeException {
	private static final long serialVersionUID = -9126373451448646241L;
	private String commandName;
	private String errorMessage;

	public DShellException(String message) {
		super(message);
		this.commandName = "";
		this.errorMessage = "";
	}

	public void setCommand(String commandName) {
		this.commandName = commandName;
	}

	public String getCommand() {
		return this.commandName;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}
}