package dshell.lang;

import dshell.annotation.Shared;
import dshell.annotation.SharedClass;

/**
 * If command invocation problem, throw this exception class instance
 * @author skgchxngsxyz-osx
 *
 */
@SharedClass("Exception")
public class DShellException extends Exception {
	private static final long serialVersionUID = -9126373451448646241L;
	private String commandName;
	private String errorMessage;

	@Shared
	public DShellException(String message) {
		super(message);
		this.commandName = "";
		this.errorMessage = "";
	}

	@Shared
	public DShellException() {
		super();
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

	@SharedClass("DShellException")
	public static class NullException extends DShellException {
		private static final long serialVersionUID = -8950050196748138954L;

		private NullException(String message) {
			super(message);
		}
	}

	public static NullException createNullException(String message) {
		return new NullException(message);
	}
}