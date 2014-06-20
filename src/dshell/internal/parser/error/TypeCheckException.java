package dshell.internal.parser.error;

import org.antlr.v4.runtime.Token;

/**
 * report type error.
 * @author skgchxngsxyz-osx
 *
 */
public class TypeCheckException extends RuntimeException {
	private static final long serialVersionUID = 5959458194095777506L;

	/**
	 * used for message foramting.
	 * may be null
	 */
	protected final Token errorPointToken;

	public TypeCheckException(Token errorPointToken, String message) {
		super(message);
		this.errorPointToken = errorPointToken;
	}

	@Override
	public String getMessage() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("[TypeError] ");
		sBuilder.append(super.getMessage());
		if(this.errorPointToken != null) {
			sBuilder.append("\n\t");
			StringBuilder subBuilder = new StringBuilder();
			subBuilder.append(this.errorPointToken.getTokenSource().getSourceName());
			subBuilder.append(':');
			subBuilder.append(this.errorPointToken.getLine());
			subBuilder.append(',');
			subBuilder.append(this.errorPointToken.getCharPositionInLine());
			subBuilder.append("   ");
			String errorLocation = subBuilder.toString();
			int size = errorLocation.length();

			String tokenText = this.errorPointToken.getText();
			sBuilder.append(errorLocation);
			sBuilder.append(tokenText);
			sBuilder.append("\n\t");
			for(int i = 0; i < size; i++) {
				sBuilder.append(' ');
			}
			int tokenSize = tokenText.length();
			for(int i = 0; i < tokenSize; i++) {
				sBuilder.append('^');
			}
		}
		return sBuilder.toString();
	}
}
