package dshell.internal.grammar;

import libbun.parser.classic.BSourceContext;
import libbun.util.BTokenFunction;

public class DoubleQuoteStringLiteralTokenFunc extends BTokenFunction {
	@Override
	public boolean Invoke(BSourceContext sourceContext) {
		int startIndex = sourceContext.GetPosition();
		sourceContext.MoveNext();
		try {
			this.matchStringLiteral(sourceContext);
			sourceContext.Tokenize(DoubleQuoteStringLiteralPatternFunc.patternName, startIndex, sourceContext.GetPosition());
			return true;
		}
		catch(Exception e) {
			sourceContext.LogWarning(startIndex, e.getMessage());
			return false;
		}
	}

	private void matchStringLiteral(BSourceContext sourceContext) {
		while(sourceContext.HasChar()) {
			char ch = sourceContext.GetCurrentChar();
			switch(ch) {
			case '\"':
				sourceContext.MoveNext();	// eat '"'
				return;
			case '\n':
				break;
			case '\\':
				sourceContext.MoveNext();
				break;
			case '$':
				this.matchDollar(sourceContext);
				break;
			}
			sourceContext.MoveNext();
		}
		throw new RuntimeException("unclosed \"");
	}

	private void matchExpression(BSourceContext sourceContext, final char openChar, final char closeChar) {
		int braceCount = 1;
		while(sourceContext.HasChar()) {
			char ch = sourceContext.GetCurrentChar();
			if(ch == '\"') {
				this.matchStringLiteral(sourceContext);
				continue;
			}
			else if(ch == openChar) {
				braceCount++;
			}
			else if(ch == closeChar) {
				if(--braceCount == 0) {
					return;
				}
			}
			else if(ch == '\\') {
				sourceContext.MoveNext();
			}
			else if(ch == '$') {
				this.matchDollar(sourceContext);
			}
			sourceContext.MoveNext();
		}
		throw new RuntimeException("unclosed " + closeChar);
	}

	private void matchDollar(BSourceContext sourceContext) {
		char ch = sourceContext.GetCharAtFromCurrentPosition(1);
		if(ch == '{' || ch == '(') {
			sourceContext.MoveNext();
			sourceContext.MoveNext();
			this.matchExpression(sourceContext, ch, ch == '{' ? '}' : ')');
		}
	}
}
