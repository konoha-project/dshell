package dshell.grammar;

import libbun.util.ZTokenFunction;
import libbun.parser.ZSourceContext;

public class ShellStyleCommentTokenFunc extends ZTokenFunction {
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
