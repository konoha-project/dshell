package dshell;

import java.io.PrintStream;
import java.util.TreeSet;

import dshell.console.DShellConsole;
import dshell.lang.DShellGrammar;
import dshell.lang.ModifiedTypeSafer;
import dshell.lib.RuntimeContext;
import dshell.lib.Utils;
import dshell.rec.RECWriter;
import dshell.remote.RequestReceiver;
import libbun.util.LibZen;
import libbun.encode.jvm.ModifiedAsmGenerator;
import libbun.parser.ZGenerator;
import static dshell.lib.RuntimeContext.AppenderType;

public class DShell {
	public final static String progName  = "D-Shell";
	public final static String codeName  = "Reference Implementation of D-Script";
	public final static int majorVersion = 0;
	public final static int minerVersion = 1;
	public final static int patchLevel   = 0;
	public final static String version = "0.2";
	public final static String copyright = "Copyright (c) 2013-2014, Konoha project authors";
	public final static String license = "BSD-Style Open Source";
	public final static String shellInfo = progName + ", version " + version + " (" + LibZen._GetPlatform() + ") powered by LibZen";

	private boolean interactiveMode = true;
	private boolean autoImportCommand = true;
	private boolean disableWelcomeMessage = false;
	private boolean recSupport = false;
	private String recURL = null;
	private String[] scriptArgs;

	private DShell(String[] args) {
		for(int i = 0; i < args.length; i++) {
			String optionSymbol = args[i];
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
					this.recSupport = true;
					this.recURL = args[++i];
				}
				else if(optionSymbol.equals("--receive") && i + 1 < args.length && args.length == 2) {	// never return
					RequestReceiver.invoke(args[++i]);
				}
				else {
					System.err.println("dshell: " + optionSymbol + ": invalid option");
					showHelpAndExit(1, System.err);
				}
			}
			else {
				this.interactiveMode = false;
				int size = args.length - i;
				this.scriptArgs = new String[size];
				System.arraycopy(args, i, this.scriptArgs, 0, size);
				break;
			}
		}
	}

	private void execute() {
		// init context
		RuntimeContext.getContext();
		if(this.recSupport) {
			if(this.interactiveMode) {
				System.err.println("dshell: need script file");
				showHelpAndExit(1, System.err);
			}
			RECWriter.invoke(this.recURL, this.scriptArgs);	// never return
		}

		ModifiedAsmGenerator generator = this.initGenerator(ModifiedAsmGenerator.class.getName(), DShellGrammar.class.getName());
		if(this.interactiveMode) {
			DShellConsole console = new DShellConsole();
			String line = null;
			if(!this.disableWelcomeMessage) {
				System.out.println(DShellConsole.welcomeMessage);
			}
			DShell.showVersionInfo();
			if(this.autoImportCommand) {
				StringBuilder importBuilder = new StringBuilder();
				importBuilder.append("command ");
				TreeSet<String> commandSet = Utils.getCommandSetFromPath();
				int size = commandSet.size();
				for(int i = 0; i < size; i++) {
					if(i != 0) {
						importBuilder.append(", ");
					}
					importBuilder.append(commandSet.pollFirst());
				}
				generator.LoadScript(importBuilder.toString(), "(stdin)", 0, true);
			}
			generator.Logger.OutputErrorsToStdErr();
			while ((line = console.readLine()) != null) {
				if(line.equals("")) {
					continue;
				}
				if(generator.LoadScript(line, "(stdin)", console.getLineNumber(), true)) {
					generator.EvalAndPrint();
				}
				console.incrementLineNum(line);
			}
			System.out.println("");
		}
		else {
			String scriptName = this.scriptArgs[0];
			// load script arguments
			StringBuilder ARGVBuilder = new StringBuilder();
			ARGVBuilder.append("let ARGV = [");
			for(int i = 0; i < this.scriptArgs.length; i++) {
				if(i != 0) {
					ARGVBuilder.append(", ");
				}
				ARGVBuilder.append("\"");
				ARGVBuilder.append(this.scriptArgs[i]);
				ARGVBuilder.append("\"");
			}
			ARGVBuilder.append("]");
			generator.LoadScript(ARGVBuilder.toString(), scriptName, 0, false);
			// load script file
			boolean status = generator.LoadFile(scriptName, null);
			if(!status) {
				System.err.println("abort loading: " + scriptName);
				System.exit(1);
			}
			generator.InvokeMain(); // never return
		}
	}

	public static void showVersionInfo() {
		System.out.println(shellInfo);
		System.out.println(copyright);
	}

	public static void showHelpAndExit(int status, PrintStream stream) {
		stream.println(shellInfo);
		stream.println("Usage: dshell [<options>] [<script-file> <argument> ...]");
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

	public final ModifiedAsmGenerator initGenerator(String ClassName, String GrammarClass) {
		ZGenerator Generator = LibZen._LoadGenerator(ClassName, null);
		LibZen._ImportGrammar(Generator.RootNameSpace, GrammarClass);
		Generator.SetTypeChecker(new ModifiedTypeSafer((ModifiedAsmGenerator) Generator));
		Generator.RequireLibrary("common", null);
		return (ModifiedAsmGenerator) Generator;
	}

	public static void main(String[] args) {
		new DShell(args).execute();
	}
}
