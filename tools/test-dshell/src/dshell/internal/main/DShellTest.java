package dshell.internal.main;

import libbun.util.LibBunSystem;
import dshell.internal.console.AbstractConsole;
import dshell.internal.jvm.Generator4Test;
import dshell.internal.lang.DShellTypeChecker;
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
		GeneratorFactory gFactory = new GeneratorFactory(Generator4Test.class, DShellTypeChecker.class);
		switch(this.mode) {
		case receiverMode:
			System.err.println("unsupported execution mode");
			System.exit(1);
		case interactiveMode:
			this.runInteractiveMode(gFactory, new DummyConsole(this.scriptArgs[0]));	// never return
		case scriptingMode:
			this.runScriptingMode(gFactory);	// never return
		case inputEvalMode:
			System.err.println("unsupported execution mode");
			System.exit(1);
		}
	}

	@Override
	protected void showVersionInfo() {	// do nothing
	}

	public static void main(String[] args) {
		new DShellTest(args).execute();
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