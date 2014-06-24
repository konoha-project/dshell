package testable;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.antlr.v4.runtime.ANTLRInputStream;

import dshell.internal.console.AbstractConsole;
import dshell.internal.exe.DShellEngineFactory.DShellExecutionEngine;
import dshell.internal.exe.EngineFactory;
import dshell.internal.exe.ExecutionEngine;
import dshell.internal.lib.RuntimeContext;
import dshell.internal.main.DShell;
//import dshell.internal.remote.RequestReceiver;

public class DShellTest extends DShell {
	public DShellTest(String[] args) {
		super(args);
	}

	@Override
	protected void parseArguments(String[] args) {
		this.autoImportCommand = false;
		this.mode = ExecutionMode.scriptingMode;
		for(int i = 0; i < args.length; i++) {
			String optionSymbol = args[i];
			if(optionSymbol.startsWith("-")) {
				if(optionSymbol.equals("-i")) {
					this.mode = ExecutionMode.interactiveMode;
				}
				else {
					System.err.println(optionSymbol + ": invalid option");
					System.exit(1);
				}
			}
			else {
				int size = args.length - i;
				this.scriptArgs = new String[size];
				System.arraycopy(args, i, this.scriptArgs, 0, size);
				break;
			}
		}
	}

	@Override
	public void execute() {
		RuntimeContext.getInstance();
		ExecutionEngine engine = new TestableEngineFactory().getEngine();
		switch(this.mode) {
		case receiverMode:
//			RequestReceiver.invoke(null);	// never return
		case interactiveMode:
			this.runInteractiveMode(engine, new DummyConsole(this.scriptArgs[0]));	// never return
		case scriptingMode:
			this.runScriptingMode(engine);	// never return
		case inputEvalMode:
			this.runInputEvalMode(null);	// never return
		}
	}

	@Override
	protected void showVersionInfo() {	// do nothing
	}

	public static void main(String[] args) {
		new DShellTest(args).execute();
	}
}

class TestableEngineFactory implements EngineFactory {
	@Override
	public ExecutionEngine getEngine() {
		return new TestableEngine();
	}

	private static class TestableEngine extends DShellExecutionEngine {
		@Override
		public void eval(String source, int lineNum) {
			ANTLRInputStream input = new ANTLRInputStream(source);
			input.name = "(stdin)";
			if(!this.eval(input, lineNum, true)) {
				/**
				 * if evaluation failed, terminates immediately.
				 */
				System.exit(1);
			}
		}
	}
}

/**
 * read from file, instead of standard input.
 * used for interactive mode test
 * @author skgchxngsxyz-osx
 *
 */
class DummyConsole extends AbstractConsole {
	private BufferedReader reader;

	public DummyConsole(String fileName) {
		this.lineNumber = 1;
		try {
			this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		} catch (FileNotFoundException e) {
			System.err.println("file not found: " + fileName);
			System.exit(1);
		}
	}

	@Override
	public String readLine() {
		return this.readLineImpl("dummy", "dummy");
	}

	@Override
	protected String readLine(String prompt) {
		try {
			return this.reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
}