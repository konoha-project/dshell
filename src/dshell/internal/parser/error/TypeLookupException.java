package dshell.internal.parser.error;

import org.antlr.v4.runtime.Token;

public class TypeLookupException extends TypeCheckException {
	private static final long serialVersionUID = -2757004654976764776L;

	private final String message;

	public TypeLookupException(String message) {
		super(null, message);
		this.message = message;
	}

	private String getSourceMessage() {
		return this.message;
	}

	public static void formateAndPropagateException(TypeLookupException e, Token token) {
		throw new FormattedTypeLookupException(e, token);
	}

	private static class FormattedTypeLookupException extends TypeCheckException {
		private static final long serialVersionUID = -7553167319361964982L;

		private final TypeLookupException cause;

		private FormattedTypeLookupException(TypeLookupException cause, Token errorPointToken) {
			super(errorPointToken, cause.getSourceMessage());
			this.cause = cause;
		}

		@Override
		public void printStackTrace() {
			this.cause.printStackTrace();
			super.printStackTrace();
		}
	}
}
