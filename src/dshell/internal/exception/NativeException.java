package dshell.internal.exception;

import dshell.internal.lib.RuntimeContext;

public class NativeException extends Exception {
	private static final long serialVersionUID = -846806025039854L;

	private final Throwable internalException;

	private NativeException(Throwable internalException) {
		this.internalException = internalException;
		this.setStackTrace(this.recreateStackTrace(this.internalException.getStackTrace()));
	}

	public static Exception wrapException(Throwable t) {
		if(t instanceof Exception) {
			return (Exception) t;
		}
		return new NativeException(t);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " -> " + this.internalException.getClass().getCanonicalName();
	}

	@Override
	protected void craeteHeader(StringBuilder sBuilder) {
		String message = this.internalException.getMessage();
		message = (message == null ? "" : message);
		sBuilder.append(this.toString() +  ": " + message + "\n");
	}

	@Override
	public void printStackTrace() {
		super.printStackTrace();
		if(RuntimeContext.getContext().isDebugMode()) {
			this.internalException.printStackTrace();
		}
	}
}
