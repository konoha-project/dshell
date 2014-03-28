package dshell.grammar;

import dshell.lang.DShellGrammar;
import libbun.util.ZTokenFunction;
import libbun.parser.ZSourceContext;

public class CommandSymbolTokenFunc extends ZTokenFunction {
	@Override
	public boolean Invoke(ZSourceContext SourceContext) {
		int startIndex = SourceContext.GetPosition();
		StringBuilder symbolBuilder = new StringBuilder();
		while(SourceContext.HasChar()) {
			char ch = SourceContext.GetCurrentChar();
			if(!Character.isLetterOrDigit(ch) && ch != '-' && ch != '+' && ch != '_') {
				break;
			}
			symbolBuilder.append(ch);
			SourceContext.MoveNext();
		}
		if(SourceContext.TokenContext.NameSpace.GetSymbol(DShellGrammar.toCommandSymbol(symbolBuilder.toString())) != null) {
			SourceContext.Tokenize(CommandSymbolPatternFunc.PatternName, startIndex, SourceContext.GetPosition());
			return true;
		}
		return false;
	}
}
