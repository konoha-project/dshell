package dshell.internal.exe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.rmi.CORBA.Util;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;

import dshell.internal.codegen.JavaByteCodeGen;
import dshell.internal.lib.DShellClassLoader;
import dshell.internal.lib.Utils;
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
			this.setSource(source, lineNum);
			ToplevelContext tree = this.parser.toplevel();
//			tree.inspect(this.parser);
			RootNode checkedNode = this.checker.checkTypeRootNode(tree.node);
			if(checkedNode == null) {
				return;
			}
			Class<?> generatedClass = this.codeGen.generateTopLevelClass(checkedNode, true);
			this.startExecution(generatedClass);
		}

		@Override
		public void loadDShellRC() {
			System.err.println("unimplemented");
		}

		protected void setSource(String source, int lineNum) {
			ANTLRInputStream input = new ANTLRInputStream(source);
			input.name = "(stdin)";
			this.setInputStream(input, lineNum);
		}

		/**
		 * set input stream to lexer and set token to parser.
		 * @param input
		 * - ANTLRInputstream
		 * @param lineNum
		 * - start line number.
		 */
		protected void setInputStream(ANTLRInputStream input, int lineNum) {
			this.lexer.setInputStream(input);
			this.lexer.setLine(lineNum);
			CommonTokenStream tokenStream = new CommonTokenStream(this.lexer);
			tokenStream.fill();
			this.parser.setTokenStream(tokenStream);
//			this.parser.setTrace(true);
		}

		/**
		 * start execution from top level class.
		 * @param entryClass
		 * - generated top level class.
		 * @return
		 * return false, if invocation target exception has raised.
		 */
		protected boolean startExecution(Class<?> entryClass) {
			try {
				Method staticMethod = entryClass.getMethod("invoke");
				staticMethod.invoke(null);
				return true;
			} catch(InvocationTargetException e) {
				Utils.printException(e);
			} catch(Throwable t) {
				t.printStackTrace();
				Utils.fatal(1, "invocation problem");
			}
			return false;
		}
	}
}
