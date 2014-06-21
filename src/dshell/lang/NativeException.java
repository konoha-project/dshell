package dshell.lang;

import dshell.annotation.Shared;
import dshell.annotation.SharedClass;
import dshell.internal.lib.RuntimeContext;

@SharedClass("Exception")
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

	@Shared
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

	@Shared
	@Override
	public void printStackTrace() {
		super.printStackTrace();
		if(RuntimeContext.getInstance().isDebugMode()) {
			this.internalException.printStackTrace();
		}
	}

	@Shared
	@Override
	public String getMessage() {
		return this.internalException.getMessage();
	}
}
