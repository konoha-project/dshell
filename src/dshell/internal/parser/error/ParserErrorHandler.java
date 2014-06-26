package dshell.internal.parser.error;

import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class ParserErrorHandler extends DefaultErrorStrategy {
	private final static ParserErrorListener listener = new ParserErrorListener();
	@Override
	public void recover(Parser recognizer, RecognitionException e) {
		throw new ParserException(e);
	}

	@Override
	public void reportError(Parser recognizer, RecognitionException e) {	//TODO: error message
		super.reportError(recognizer, e);
	}

	public static void reportError(RecognitionException e) {
		throw new ParserException(e);
	}

	public static class ParserException extends RuntimeException {	// TODO: error message
		private static final long serialVersionUID = 7581149595021342812L;

		private ParserException(RecognitionException e) {
			super(e);
		}
	}

	private static class ParserErrorListener extends ConsoleErrorListener {	//TODO: error message
		@Override
		public void syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line, 
				int charPositionInLine, String msg, RecognitionException e) {
			super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
			reportError(e);
		}
	}

	public static ParserErrorListener getErrorListener() {
		return listener;
	}
}
