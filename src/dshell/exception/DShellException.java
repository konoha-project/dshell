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

	@Override
	public String toString() {
		String name = this.getClass().getSimpleName();
		return name + ": " + this.getMessage();
	}

	@Override
	public void printStackTrace() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append(this.toString() + "\n");
		for(StackTraceElement element : this.getStackTrace()) {
			sBuilder.append("\tfrom ");
			sBuilder.append(element.getFileName());
			sBuilder.append(":");
			sBuilder.append(element.getLineNumber());
			sBuilder.append(" '");
			sBuilder.append(this.formateMethodName(element.getClassName(), element.getMethodName()));
			sBuilder.append("'\n");
		}
		System.err.print(sBuilder.toString());
	}

	private String formateMethodName(String className, String methodName) {
		if(!methodName.equals("f")) {
			return className + "." + methodName;
		}
		String[] splitStrings = className.split("__");
		String name = splitStrings[0].substring(1);
		if(name.startsWith("Main") || name.equals("main")) {
			return "TopLevel";
		}
		return "function " + name + "()";
	}

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