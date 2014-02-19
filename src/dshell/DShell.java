package dshell;

import java.io.PrintStream;

import dshell.lang.DShellGrammar;
import dshell.lib.RuntimeContext;
import dshell.rec.RECWriter;
import dshell.remote.RequestReceiver;
import dshell.util.DShellConsole;
import zen.codegen.jvm.ModifiedAsmGenerator;
import zen.deps.KonohaGrammar;
import zen.deps.LibZen;
import zen.main.ZenMain;
import zen.parser.ZGenerator;
import zen.parser.ZSourceEngine;
import static dshell.lib.RuntimeContext.AppenderType;

public class DShell {
	public final static String progName  = "D-Shell";
	public final static String codeName  = "Reference Implementation of D-Script";
	public final static int majorVersion = 0;
	public final static int minerVersion = 1;
	public final static int patchLevel   = 0;
	public final static String version = "0.1";
	public final static String copyright = "Copyright (c) 2013-2014, Konoha project authors";
	public final static String license = "BSD-Style Open Source";
	public final static String shellInfo = progName + ", version " + version + " (" + LibZen._GetPlatform() + ")";

	private boolean interactiveMode = true;
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
		ZSourceEngine engine = loadDShellEngine();
		if(this.recSupport) {
			if(this.interactiveMode) {
				System.err.println("dshell: need script file");
				showHelpAndExit(1, System.err);
			}
			RECWriter.invoke(this.recURL, this.scriptArgs);	// never return
		}
		if(this.interactiveMode) {
			DShellConsole console = new DShellConsole();
			showVersionInfo();
			engine.Generator.Logger.ShowErrors();
			int linenum = 1;
			String line = null;
			while ((line = console.readLine()) != null) {
				if(line.trim().equals("")) {
					continue;
				}
				try {
					Object evaledValue = engine.Eval(line, "(stdin)", linenum, this.interactiveMode);
					engine.Generator.Logger.ShowErrors();
					if (LibZen.DebugMode && evaledValue != null) {
						System.out.print(" (" + /*ZSystem.GuessType(evaledValue)*/ ":");
						System.out.print(LibZen.GetClassName(evaledValue)+ ") ");
						System.out.println(LibZen._Stringify(evaledValue));
					}
				}
				catch (Exception e) {
					ZenMain.PrintStackTrace(e, linenum);
				}
				linenum++;
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
			engine.Eval(ARGVBuilder.toString(), scriptName, 0, false);
			// load script file
			boolean status = engine.Load(scriptName);
			engine.Generator.Logger.ShowErrors();
			if(!status) {
				System.err.println("abort loading: " + scriptName);
				System.exit(1);
			}
			System.exit(0);
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
		stream.println("    --help");
		stream.println("    --logging:file [file path (appendable)]");
		stream.println("    --logging:stdout");
		stream.println("    --logging:stderr");
		stream.println("    --logging:syslog [host address]");
		stream.println("    --rec [rec URL]");
		stream.println("    --version");
		System.exit(status);
	}

	private final static ZSourceEngine loadDShellEngine() {
		ZGenerator generator = new ModifiedAsmGenerator();
		LibZen.ImportGrammar(generator.RootNameSpace, DShellGrammar.class.getName());
		return generator.GetEngine();
	}

	public static void main(String[] args) {
		new DShell(args).execute();
	}
}
