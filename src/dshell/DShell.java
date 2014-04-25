package dshell;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.TreeSet;

import dshell.console.DShellConsole;
import dshell.grammar.DShellGrammar;
import dshell.lang.DShellTypeChecker;
import dshell.lib.RuntimeContext;
import dshell.lib.Utils;
import dshell.rec.RECWriter;
import dshell.remote.RequestReceiver;
import libbun.util.LibBunSystem;
import libbun.encode.jvm.DShellByteCodeGenerator;
import static dshell.lib.RuntimeContext.AppenderType;

public class DShell {
	public final static String progName  = "D-Shell";
	public final static String codeName  = "Reference Implementation of D-Script";
	public final static int majorVersion = 0;
	public final static int minerVersion = 1;
	public final static int patchLevel   = 0;
	public final static String version = "0.3-unstable";
	public final static String copyright = "Copyright (c) 2013-2014, Konoha project authors";
	public final static String license = "BSD-Style Open Source";
	public final static String shellInfo = progName + ", version " + version + " (" + LibBunSystem._GetPlatform() + ") powered by LibBun";

	public static enum ExecutionMode {
		interactiveMode,
		scriptingMode,
		inputEvalMode,
		recSupportMode,
		receiverMode,
	}

	private ExecutionMode mode;
	private boolean autoImportCommand = true;
	private boolean disableWelcomeMessage = false;
	private final boolean enableDummyTerminal;
	private String specificArg = null;
	private String[] scriptArgs;

	private DShell(String[] args) {
		this(args, false);
	}

	public DShell(String[] args, boolean enableDummyTerminal) {
		this.enableDummyTerminal = enableDummyTerminal;
		this.parseArguments(args);
	}

	private void parseArguments(String[] args) {
		boolean foundScript = false;
		boolean recSupport = false;
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
				else if(optionSymbol.equals("--disable-welcome")) {
					this.disableWelcomeMessage = true;
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
				else if(optionSymbol.equals("--rec") && i + 1 < args.length) {
					recSupport = true;
					this.specificArg = args[++i];
				}
				else if(optionSymbol.equals("--receive") && i + 1 < args.length && args.length == 2) {	// never return
					this.mode = ExecutionMode.receiverMode;
					this.specificArg = args[++i];
					return;
				}
				else {
					System.err.println("dshell: " + optionSymbol + ": invalid option");
					showHelpAndExit(1, System.err);
				}
			}
			else if(optionSymbol.startsWith("-")) {
				if(optionSymbol.equals("-c") && i + 1 == args.length - 1 && !recSupport) {
					this.mode = ExecutionMode.inputEvalMode;
					this.specificArg = args[++i];
					return;
				}
				else {
					System.err.println("dshell: " + optionSymbol + ": invalid option");
					showHelpAndExit(1, System.err);
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
		if(recSupport) {
			if(!foundScript) {
				System.err.println("dshell: need script file");
				showHelpAndExit(1, System.err);
			}
			this.mode = ExecutionMode.recSupportMode;
		}
		else if(foundScript) {
			this.mode = ExecutionMode.scriptingMode;
		}
		else if(!this.enableDummyTerminal && System.console() == null) {
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
		// init context
		RuntimeContext.getContext();

		switch(this.mode) {
		case recSupportMode:
			RECWriter.invoke(this.specificArg, this.scriptArgs);	// never return
		case receiverMode:
			RequestReceiver.invoke(this.specificArg);	// never return
		case interactiveMode:
			this.runInteractiveMode();	// never return
		case scriptingMode:
			this.runScriptingMode();	// never return
		case inputEvalMode:
			this.runInputEvalMode();	// never return
		}
	}

	public void runInteractiveMode() {
		DShellByteCodeGenerator generator = initGenerator();
		DShellConsole console = new DShellConsole();
		String line = null;
		if(!this.disableWelcomeMessage) {
			System.out.println(DShellConsole.welcomeMessage);
		}
		DShell.showVersionInfo();
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
			generator.loadLine(importBuilder.toString(), 0, false);
		}
		generator.Logger.OutputErrorsToStdErr();
		while ((line = console.readLine()) != null) {
			if(line.equals("")) {
				continue;
			}
			if(generator.loadLine(line, console.getLineNumber(), true)) {
				generator.evalAndPrint();
			}
			console.incrementLineNum(line);
		}
		System.out.println("");
		System.exit(0);
	}

	private void runScriptingMode() {
		DShellByteCodeGenerator generator = initGenerator();
		String scriptName = this.scriptArgs[0];
		generator.loadArg(this.scriptArgs);
		boolean status = generator.loadFile(scriptName);
		if(!status) {
			System.err.println("abort loading: " + scriptName);
			System.exit(1);
		}
		generator.invokeMain(); // never return
	}

	private void runInputEvalMode() {
		DShellByteCodeGenerator generator = initGenerator();
		String source = this.specificArg;
		if(this.specificArg == null) {
			source = readFromIntput();
		}
		boolean status = generator.loadLine(source, 1, false);
		if(!status) {
			System.err.println("abort loading input source");
			System.exit(1);
		}
		generator.invokeMain(); // never return
	}

	private static String readFromIntput() {
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
		catch (IOException e) {
			e.printStackTrace();
			Utils.fatal(1, "IO problem");
		}
		return null;
	}
	private final static DShellByteCodeGenerator initGenerator() {
		DShellByteCodeGenerator Generator = new DShellByteCodeGenerator();
		DShellGrammar.ImportGrammar(Generator.RootGamma);
		Generator.SetTypeChecker(new DShellTypeChecker((DShellByteCodeGenerator) Generator));
		Generator.RequireLibrary("common", null);
		return (DShellByteCodeGenerator) Generator;
	}

	public static void showVersionInfo() {
		System.out.println(shellInfo);
		System.out.println(copyright);
	}

	public static void showHelpAndExit(int status, PrintStream stream) {
		stream.println(shellInfo);
		stream.println("Usage: dshell [<options>] [<script-file> <argument> ...]");
		stream.println("Usage: dshell [<options>] -c [<command>]");
		stream.println("Options:");
		stream.println("    --debug");
		stream.println("    --disable-auto-import");
		stream.println("    --disable-welcome");
		stream.println("    --help");
		stream.println("    --logging:file [file path (appendable)]");
		stream.println("    --logging:stdout");
		stream.println("    --logging:stderr");
		stream.println("    --logging:syslog [host address]");
		stream.println("    --rec [rec URL]");
		stream.println("    --version");
		System.exit(status);
	}

	public static void main(String[] args) {
		new DShell(args).execute();
	}
}
