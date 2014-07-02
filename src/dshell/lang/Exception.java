package dshell.lang;

import java.util.LinkedList;

import dshell.annotation.Shared;
import dshell.annotation.SharedClass;

/**
 * D-shell basis exception class
 * @author skgchxngsxyz-osx
 *
 */
@SharedClass
public class Exception extends RuntimeException {
	private static final long serialVersionUID = -8494693504521057747L;

	@Shared
	public Exception() {
		super();
	}

	@Shared
	public Exception(String message) {
		super(message);
	}

	@Shared
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	@Shared
	@Override
	public String getMessage() {
		return super.getMessage();
	}

	@Shared
	@Override
	public void printStackTrace() {
		StringBuilder sBuilder = new StringBuilder();
		this.createHeader(sBuilder);
		for(StackTraceElement element : this.getStackTrace()) {
			sBuilder.append("\tfrom ");
			sBuilder.append(element.getFileName());
			sBuilder.append(":");
			sBuilder.append(element.getLineNumber());
			sBuilder.append(" '");
			sBuilder.append(this.formateName(element));
			sBuilder.append("'\n");
		}
		System.err.print(sBuilder.toString());
	}

	@Override
	public Throwable fillInStackTrace() {
		super.fillInStackTrace();
		this.setStackTrace(this.recreateStackTrace(super.getStackTrace()));
		return this;
	}

	protected StackTraceElement[] recreateStackTrace(StackTraceElement[] originalElements) {
		LinkedList<StackTraceElement> elementStack = new LinkedList<StackTraceElement>();
		boolean foundNativeMethod = false;
		for(int i = originalElements.length - 1; i > -1; i--) {
			StackTraceElement element = originalElements[i];
			if(!foundNativeMethod && element.isNativeMethod()) {
				foundNativeMethod = true;
				continue;
			}
			if(foundNativeMethod && element.getClassName().startsWith("dshell.defined.")) {
				elementStack.add(element);
			}
		}
		int size = elementStack.size();
		StackTraceElement[] elements = new StackTraceElement[size];
		for(int i = 0; i < size; i++) {
			elements[i] = elementStack.pollLast();
		}
		return elements;
	}

	protected void createHeader(StringBuilder sBuilder) {
		String message = this.getMessage();
		message = (message == null ? "" : message);
		sBuilder.append(this.toString() +  ": " + message + "\n");
	}

	private String formateName(StackTraceElement element) {
		String fullyQualifiedClassName = element.getClassName();
		int index = fullyQualifiedClassName.lastIndexOf('.');
		String className = fullyQualifiedClassName.substring(index + 1);
		if(fullyQualifiedClassName.startsWith("dshell.defined.toplevel")) {
			return "<toplevel>()";
//		} else if(fullyQualifiedClassName.startsWith("dshell.defined.class")) { //TODO:
			
		} else if(fullyQualifiedClassName.startsWith("dshell.defined.func")) {
			int prefixIndex = className.indexOf('_');
			return className.substring(prefixIndex + 1) + "()";
		}
		return "unknown";
	}
}
