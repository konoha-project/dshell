package dshell.grammar;

import libbun.parser.BSourceContext;
import libbun.util.BTokenFunction;

public class SingleQuoteStringLiteralTokenFunc extends BTokenFunction {
	@Override
	public boolean Invoke(BSourceContext sourceContext) {
		int startIndex = sourceContext.GetPosition();
		sourceContext.MoveNext();
		while(sourceContext.HasChar()) {
			char ch = sourceContext.GetCurrentChar();
			if(ch == '\'') {
				sourceContext.MoveNext(); // eat '
				sourceContext.Tokenize(SingleQuoteStringLiteralPatternFunc.patternName, startIndex, sourceContext.GetPosition());
				return true;
			}
			if(ch == '\n') {
				break;
			}
			if(ch == '\\') {
				sourceContext.MoveNext();
			}
			sourceContext.MoveNext();
		}
		sourceContext.LogWarning(startIndex, "unclosed \'");
		return false;
	}
}
