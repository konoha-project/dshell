package dshell.grammar;

import zen.util.ZTokenFunction;
import zen.parser.ZSourceContext;

public class ShellStyleCommentToken extends ZTokenFunction {
	@Override
	public boolean Invoke(ZSourceContext SourceContext) {
		while(SourceContext.HasChar()) {
			char ch = SourceContext.GetCurrentChar();
			if(ch == '\n') {
				break;
			}
			SourceContext.MoveNext();
		}
		return true;
	}


}
