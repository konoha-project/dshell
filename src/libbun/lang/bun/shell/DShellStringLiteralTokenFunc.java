package libbun.lang.bun.shell;

import libbun.parser.BSourceContext;
import libbun.util.BTokenFunction;

public class DShellStringLiteralTokenFunc extends BTokenFunction {
	@Override
	public boolean Invoke(BSourceContext SourceContext) {
		int StartIndex = SourceContext.GetPosition();
		SourceContext.MoveNext();
		try {
			this.MatchStringLiteral(SourceContext);
			SourceContext.Tokenize(DShellStringLiteralPatternFunc.PatternName, StartIndex, SourceContext.GetPosition());
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

	private void MatchExpression(BSourceContext SourceContext) {
		int braceCount = 1;
		while(SourceContext.HasChar()) {
			char ch = SourceContext.GetCurrentChar();
			switch(ch) {
			case '\"':
				this.MatchStringLiteral(SourceContext);
				continue;
			case '{':
				braceCount++;
				break;
			case '}':
				if(--braceCount == 0) {
					return;
				}
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
		throw new RuntimeException("unclosed }");
	}

	private void MatchCommandSubstitution(BSourceContext SourceContext) {
		int braceCount = 1;
		while(SourceContext.HasChar()) {
			char ch = SourceContext.GetCurrentChar();
			switch(ch) {
			case '\"':
				this.MatchStringLiteral(SourceContext);
				continue;
			case '(':
				braceCount++;
				break;
			case ')':
				if(--braceCount == 0) {
					return;
				}
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
		throw new RuntimeException("unclosed )");
	}

	private void MatchDollar(BSourceContext SourceContext) {
		char ch = SourceContext.GetCharAtFromCurrentPosition(1);
		if(ch == '{') {
			SourceContext.MoveNext();
			SourceContext.MoveNext();
			this.MatchExpression(SourceContext);
		}
		else if(ch == '(') {
			SourceContext.MoveNext();
			SourceContext.MoveNext();
			this.MatchCommandSubstitution(SourceContext);
		}
	}
}
