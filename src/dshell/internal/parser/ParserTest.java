package dshell.internal.parser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;

import dshell.internal.parser.Node.RootNode;
import dshell.internal.parser.dshellParser.ToplevelContext;

public class ParserTest {

	public static void main(String[] args) {
		//String ex = "function f(a : int, b : Func<int, [int]> ) { return 12 ;}";
		//String ex = "while(true) {var a = 1 \n;\n; a = 12 + 12;} var a = 12; var a = 12\n";
		//String ex = "var a : Func<int, [int, int]> = null;";
		//String ex = "12 +??+ 12";
		//String ex = "12 * 12 + 12 / 3 instanceof int";
		//String ex = "for(var i = 0; i < 12; i++) { return 12; }";
		String ex = "assert(true == false);";
		ANTLRInputStream input = new ANTLRInputStream(ex);
		Lexer lexer = new dshellLexer(null);
		lexer.setInputStream(input);
		CommonTokenStream tokenStream = new CommonTokenStream(lexer);
		tokenStream.fill();
		dshellParser parser = new dshellParser(null);
		parser.setTokenStream(tokenStream);
		parser.setTrace(true);
		ToplevelContext tree = parser.toplevel();
		tree.inspect(parser);
		System.out.println(tree.toStringTree(parser));
		TypePool pool = new TypePool();
		TypeChecker checker = new TypeChecker(pool);
		RootNode checkedNode = checker.checkTypeRootNode(tree.node);
	}
}
