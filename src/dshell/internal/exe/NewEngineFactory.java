package dshell.internal.exe;

import java.lang.reflect.Method;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;

import dshell.internal.codegen.JavaByteCodeGen;
import dshell.internal.lib.DShellClassLoader;
import dshell.internal.parser.TypeChecker;
import dshell.internal.parser.TypePool;
import dshell.internal.parser.dshellLexer;
import dshell.internal.parser.dshellParser;
import dshell.internal.parser.Node.RootNode;
import dshell.internal.parser.dshellParser.ToplevelContext;

public class NewEngineFactory implements EngineFactory {
	@Override
	public ExecutionEngine getEngine() {
		return new NewExecutionEngine();
	}

	private static class NewExecutionEngine implements ExecutionEngine {
		protected final Lexer lexer;
		protected final dshellParser parser;
		protected final DShellClassLoader classLoader;
		protected final TypeChecker checker;
		protected final JavaByteCodeGen codeGen;

		private NewExecutionEngine() {
			this.lexer = new dshellLexer(null);
			this.parser = new dshellParser(null);
			this.classLoader = new DShellClassLoader();
			this.checker = new TypeChecker(new TypePool(this.classLoader));
			this.codeGen = new JavaByteCodeGen(this.classLoader);
		}

		@Override
		public void setArg(String[] scriptArgs) {
			throw new RuntimeException("unimplemented");
		}

		@Override
		public void eval(String scriptName) {
			throw new RuntimeException("unimplemented");
		}

		@Override
		public void eval(String scriptName, String source) {
			throw new RuntimeException("unimplemented");
		}

		@Override
		public void eval(String source, int lineNum) {
			ANTLRInputStream input = new ANTLRInputStream(source);
			this.lexer.setInputStream(input);
			this.lexer.setLine(lineNum);
			CommonTokenStream tokenStream = new CommonTokenStream(lexer);
			tokenStream.fill();
			this.parser.setTokenStream(tokenStream);
			parser.setTrace(true);
			
			try {
				ToplevelContext tree = this.parser.toplevel();
				tree.inspect(parser);
				System.out.println(tree.toStringTree(parser));
				RootNode checkedNode = checker.checkTypeRootNode(tree.node);
				Class<?> generatedClass = this.codeGen.generateTopLevelClass(checkedNode, true);
				Method staticMethod = generatedClass.getMethod("invoke");
				staticMethod.invoke(null);
			}
			catch(Throwable t) {
				t.printStackTrace();
			}
		}

		@Override
		public void loadDShellRC() {
			System.err.println("unimplemented");
		}
	}
}
