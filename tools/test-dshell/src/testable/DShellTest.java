package testable;

import java.util.ArrayList;

import libbun.ast.BNode;
import libbun.util.LibBunSystem;
import dshell.internal.console.AbstractConsole;
import dshell.internal.exe.EngineFactory;
import dshell.internal.exe.ExecutionEngine;
import dshell.internal.grammar.DShellGrammar;
import dshell.internal.jvm.JavaByteCodeGenerator;
import dshell.internal.lib.RuntimeContext;
import dshell.internal.main.DShell;

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
		RuntimeContext.getContext();
		ExecutionEngine engine = new TestableEngineFactory().getEngine();
		this.execute(engine, new DummyConsole(this.scriptArgs[0]));
	}

	@Override
	protected void showVersionInfo() {	// do nothing
	}

	public static void main(String[] args) {
		new DShellTest(args).execute();
	}
}

class TestableEngine extends ExecutionEngine {
	TestableEngine(JavaByteCodeGenerator generator) {
		super(generator);
	}

	@Override
	public void eval(String sourceName, String source, int lineNum) {
		this.loadGlobalVariable(true);
		// parse script
		ArrayList<BNode> nodeList = this.parseScript(source, sourceName, lineNum);
		if(nodeList == null) {
			return;
		}
		for(BNode node : nodeList) {
			if(!this.generateStatement(node, true) || !this.invokeStatement()) {
				this.generator.Logger.OutputErrorsToStdErr();
				System.exit(1);
			}
		}
	}
}

class TestableEngineFactory extends EngineFactory {
	@Override
	public ExecutionEngine getEngine() {
		if(this.called) {
			throw new RuntimeException("already called");
		}
		this.called = true;
		JavaByteCodeGenerator generator = this.newGenerator();
		DShellGrammar.ImportGrammar(generator.RootGamma);
		generator.SetTypeChecker(newTypeChecker(generator));
		generator.RequireLibrary("common", null);
		return new TestableEngine(generator);
	}
}

class DummyConsole implements AbstractConsole {
	private String script;
	private boolean called = false;

	public DummyConsole(String fileName) {
		this.script = LibBunSystem._LoadTextFile(fileName);
		if(this.script == null) {
			System.err.println("file not found: " + fileName);
			System.exit(1);
		}
	}

	@Override
	public int getLineNumber() {
		return 1;
	}

	@Override
	public void incrementLineNum(String line) {
		// do nothing
	}

	@Override
	public String readLine() {
		if(!this.called) {
			this.called = true;
			return this.script;
		}
		return null;
	}
}