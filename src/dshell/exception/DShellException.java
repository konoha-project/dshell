package dshell.exception;

import java.util.LinkedList;

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
		return this.getClass().getSimpleName();
	}

	@Override
	public void printStackTrace() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append(this.toString() +  ": " + this.getErrorMessage() + "\n");
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

	@Override
	public Throwable fillInStackTrace() {
		super.fillInStackTrace();
		StackTraceElement[] originalElements = this.getStackTrace();
		LinkedList<StackTraceElement> elementStack = new LinkedList<StackTraceElement>();
		boolean foundNativeMethod = false;
		for(int i = originalElements.length - 1; i > -1; i--) {
			StackTraceElement element = originalElements[i];
			if(!foundNativeMethod && element.isNativeMethod()) {
				foundNativeMethod = true;
				continue;
			}
			if(foundNativeMethod && element.getMethodName().equals("f")) {
				elementStack.add(element);
			}
		}
		int size = elementStack.size();
		StackTraceElement[] elements = new StackTraceElement[size];
		for(int i = 0; i < size; i++) {
			elements[i] = elementStack.pollLast();
		}
		this.setStackTrace(elements);
		return this;
	}

	private String formateMethodName(String className, String methodName) {
		if(!methodName.equals("f")) {
			return className + "." + methodName;
		}
		String[] splitStrings = className.split("__");
		String name = splitStrings[1];
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