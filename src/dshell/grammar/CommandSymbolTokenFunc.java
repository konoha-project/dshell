package dshell.grammar;

import dshell.lang.DShellGrammar;
import libbun.util.Var;
import libbun.util.ZTokenFunction;
import libbun.parser.ZSourceContext;

public class CommandSymbolTokenFunc extends ZTokenFunction {
	@Override public boolean Invoke(ZSourceContext SourceContext) {
		@Var int startIndex = SourceContext.GetPosition();
		@Var StringBuilder symbolBuilder = new StringBuilder();
		while(SourceContext.HasChar()) {
			@Var char ch = SourceContext.GetCurrentChar();
			if(!Character.isLetterOrDigit(ch) && ch != '-' && ch != '+' && ch != '_') {
				break;
			}
			symbolBuilder.append(ch);
			SourceContext.MoveNext();
		}
		if(SourceContext.TokenContext.NameSpace.GetSymbol(DShellGrammar.toCommandSymbol(symbolBuilder.toString())) != null) {
			SourceContext.Tokenize(CommandSymbolPatternFunc._PatternName, startIndex, SourceContext.GetPosition());
			return true;
		}
		return false;
	}
}
