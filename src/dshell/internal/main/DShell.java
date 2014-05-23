package dshell.internal.main;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.TreeSet;

import dshell.internal.console.AbstractConsole;
import dshell.internal.console.DShellConsole;
import dshell.internal.exe.EngineFactory;
import dshell.internal.exe.ExecutionEngine;
import dshell.internal.lib.RuntimeContext;
import dshell.internal.lib.Utils;
import dshell.internal.remote.RequestReceiver;
import libbun.util.LibBunSystem;
import static dshell.internal.lib.RuntimeContext.AppenderType;

public class DShell {
	public final static String progName  = "D-Shell";
	public final static String codeName  = "Reference Implementation of D-Script";
	public final static int majorVersion = 0;
	public final static int minerVersion = 1;
	public final static int patchLevel   = 0;
	public final static String version = "0.4-unstable";
	public final static String copyright = "Copyright (c) 2013-2014, Konoha project authors";
	public final static String license = "BSD-Style Open Source";
	public final static String shellInfo = progName + ", version " + version + " (" + LibBunSystem._GetPlatform() + ") powered by LibBun";

	public static enum ExecutionMode {
		interactiveMode,
		scriptingMode,
		inputEvalMode,
		receiverMode,
	}

	protected ExecutionMode mode;
	protected boolean autoImportCommand = true;
	private final boolean enablePseudoTerminal;
	private String specificArg = null;
	protected String[] scriptArgs;

	public DShell(String[] args) {
		this(args, false);
	}

	public DShell(String[] args, boolean enablePseudoTerminal) {
		this.enablePseudoTerminal = enablePseudoTerminal;
		this.parseArguments(args);
	}

	protected void parseArguments(String[] args) {
		boolean foundScript = false;
		HashMap<String, Integer> foundArgMap = new HashMap<String, Integer>();
		for(int i = 0; i < args.length; i++) {
			String optionSymbol = args[i];
			this.checkDuplicatedArg(foundArgMap, optionSymbol, i);
			if(optionSymbol.startsWith("--")) {
				if(optionSymbol.equals("--version")) {
					showVersionInfo();
					System.exit(0);
				}
				else if(optionSymbol.equals("--debug")) {
					RuntimeContext.getContext().setDebugMode(true);
				}
				else if(optionSymbol.equals("--disable-auto-import")) {
					this.autoImportCommand = false;
				}
				else if(optionSymbol.equals("--help")) {
					showHelpAndExit(0, System.out);
				}
				else if(optionSymbol.equals("--logging:file") && i + 1 < args.length) {
					RuntimeContext.getContext().changeAppender(AppenderType.file, args[++i]);
				}
				else if(optionSymbol.equals("--logging:stdout")) {
					RuntimeContext.getContext().changeAppender(AppenderType.stdout);
				}
				else if(optionSymbol.equals("--logging:stderr")) {
					RuntimeContext.getContext().changeAppender(AppenderType.stderr);
				}
				else if(optionSymbol.equals("--logging:syslog")) {
					int nextIndex = i + 1;
					if(nextIndex < args.length && !args[nextIndex].startsWith("--")) {
						RuntimeContext.getContext().changeAppender(AppenderType.syslog, args[nextIndex]);
						i++;
					}
					else {
						RuntimeContext.getContext().changeAppender(AppenderType.syslog);
					}
				}
				else if(optionSymbol.equals("--receive") && i + 1 < args.length && args.length == 2) {	// never return
					this.mode = ExecutionMode.receiverMode;
					this.specificArg = args[++i];
					return;
				}
				else {
					System.err.println("dshell: " + optionSymbol + ": invalid option");
					this.showHelpAndExit(1, System.err);
				}
			}
			else if(optionSymbol.startsWith("-")) {
				if(optionSymbol.equals("-c") && i + 1 == args.length - 1) {
					this.mode = ExecutionMode.inputEvalMode;
					this.specificArg = args[++i];
					return;
				}
				else {
					System.err.println("dshell: " + optionSymbol + ": invalid option");
					this.showHelpAndExit(1, System.err);
				}
			}
			else {
				foundScript = true;
				int size = args.length - i;
				this.scriptArgs = new String[size];
				System.arraycopy(args, i, this.scriptArgs, 0, size);
				break;
			}
		}
		if(foundScript) {
			this.mode = ExecutionMode.scriptingMode;
		}
		else if(!this.enablePseudoTerminal && System.console() == null) {
			this.mode = ExecutionMode.inputEvalMode;
		}
		else {
			this.mode = ExecutionMode.interactiveMode;
		}
	}

	private void checkDuplicatedArg(HashMap<String, Integer> foundArgMap, String arg, int index) {
		if(foundArgMap.containsKey(arg)) {
			System.err.println("dshell: " + arg + ": duplicated option");
			showHelpAndExit(1, System.err);
		}
		foundArgMap.put(arg, index);
	}

	public void execute() {
		RuntimeContext.getContext();
		ExecutionEngine engine = new EngineFactory().getEngine();
		switch(this.mode) {
		case receiverMode:
			RequestReceiver.invoke(this.specificArg);	// never return
		case interactiveMode:
			this.runInteractiveMode(engine, new DShellConsole());	// never return
		case scriptingMode:
			this.runScriptingMode(engine);	// never return
		case inputEvalMode:
			this.runInputEvalMode(engine);	// never return
		}
	}

	protected void runInteractiveMode(ExecutionEngine engine, AbstractConsole console) {
		String line = null;
		this.showVersionInfo();
		if(this.autoImportCommand) {
			StringBuilder importBuilder = new StringBuilder();
			importBuilder.append("import command ");
			TreeSet<String> commandSet = Utils.getCommandSetFromPath();
			int size = commandSet.size();
			for(int i = 0; i < size; i++) {
				if(i != 0) {
					importBuilder.append(", ");
				}
				importBuilder.append(commandSet.pollFirst());
			}
			engine.eval(importBuilder.toString(), 0);
		}
		engine.loadDShellRC();
		while((line = console.readLine()) != null) {
			if(line.equals("")) {
				continue;
			}
			engine.eval(line, console.getLineNumber());
			console.incrementLineNum(line);
		}
		System.out.println("");
		System.exit(0);
	}

	protected void runScriptingMode(ExecutionEngine engine) {
		engine.setArg(this.scriptArgs);
		engine.eval(this.scriptArgs[0]);
	}

	protected void runInputEvalMode(ExecutionEngine engine) {
		if(this.specificArg != null) {
			engine.eval("(command line)", this.specificArg);
		}
		else {
			engine.eval("(stdin)", this.readFromIntput());
		}
	}

	protected String readFromIntput() {
		BufferedInputStream stream = new BufferedInputStream(System.in);
		ByteArrayOutputStream streamBuffer = new ByteArrayOutputStream();
		int bufferSize = 2048;
		int read = 0;
		byte[] buffer = new byte[bufferSize];
		try {
			while((read = stream.read(buffer, 0, bufferSize)) > -1) {
				streamBuffer.write(buffer, 0, read);
			}
			return streamBuffer.toString();
		}
		catch(IOException e) {
			e.printStackTrace();
			Utils.fatal(1, "IO problem");
		}
		return null;
	}

	protected void showVersionInfo() {
		System.out.println(shellInfo);
		System.out.println(copyright);
	}

	protected void showHelpAndExit(int status, PrintStream stream) {
		stream.println(shellInfo);
		stream.println("Usage: dshell [<options>] [<script-file> <argument> ...]");
		stream.println("Usage: dshell [<options>] -c [<command>]");
		stream.println("Options:");
		stream.println("    --debug");
		stream.println("    --disable-auto-import");
		stream.println("    --help");
		stream.println("    --logging:file [file path (appendable)]");
		stream.println("    --logging:stdout");
		stream.println("    --logging:stderr");
		stream.println("    --logging:syslog [host address]");
		stream.println("    --version");
		System.exit(status);
	}

	public static void main(String[] args) {
		new DShell(args).execute();
	}
}
