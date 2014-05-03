package dshell.grammar;

import libbun.parser.BSourceContext;
import libbun.util.BTokenFunction;

public class DoubleQuoteStringLiteralTokenFunc extends BTokenFunction {
	@Override
	public boolean Invoke(BSourceContext SourceContext) {
		int StartIndex = SourceContext.GetPosition();
		SourceContext.MoveNext();
		try {
			this.MatchStringLiteral(SourceContext);
			SourceContext.Tokenize(DoubleQuoteStringLiteralPatternFunc.PatternName, StartIndex, SourceContext.GetPosition());
			return true;
		}
		catch(Exception e) {
			SourceContext.LogWarning(StartIndex, e.getMessage());
			return false;
		}
	}

	private void MatchStringLiteral(BSourceContext SourceContext) {
		while(SourceContext.HasChar()) {
			char ch = SourceContext.GetCurrentChar();
			switch(ch) {
			case '\"':
				SourceContext.MoveNext();	// eat '"'
				return;
			case '\n':
				break;
			case '\\':
				SourceContext.MoveNext();
				break;
			case '$':
				this.MatchDollar(SourceContext);
				break;
			}
			SourceContext.MoveNext();
		}
		throw new RuntimeException("unclosed \"");
	}

	private void MatchExpression(BSourceContext SourceContext, final char openChar, final char closeChar) {
		int braceCount = 1;
		while(SourceContext.HasChar()) {
			char ch = SourceContext.GetCurrentChar();
			if(ch == '\"') {
				this.MatchStringLiteral(SourceContext);
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
				SourceContext.MoveNext();
			}
			else if(ch == '$') {
				this.MatchDollar(SourceContext);
			}
			SourceContext.MoveNext();
		}
		throw new RuntimeException("unclosed " + closeChar);
	}

	private void MatchDollar(BSourceContext SourceContext) {
		char ch = SourceContext.GetCharAtFromCurrentPosition(1);
		if(ch == '{' || ch == '(') {
			SourceContext.MoveNext();
			SourceContext.MoveNext();
			this.MatchExpression(SourceContext, ch, ch == '{' ? '}' : ')');
		}
	}
}
