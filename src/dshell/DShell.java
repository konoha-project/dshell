package dshell;

import java.io.PrintStream;

import dshell.lang.DShellGrammar;
import dshell.remote.DShellDaemon;
import dshell.util.DShellConsole;
import dshell.util.LoggingContext;
import dshell.util.Utils;
import zen.codegen.jvm.ModifiedAsmGenerator;
import zen.deps.LibZen;
import zen.deps.ZenArray;
import zen.lang.ZenEngine;
import zen.main.ZenMain;
import zen.parser.ZGenerator;
import zen.type.ZType;
import static dshell.util.LoggingContext.AppenderType;

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
	private boolean debugMode = false;
	private ZenArray<String> ARGV;

	private DShell(String[] args) {
		boolean foundScriptFile = false;
		for(int i = 0; i < args.length; i++) {
			String optionSymbol = args[i];
			if(!foundScriptFile && optionSymbol.startsWith("--")) {
				if(optionSymbol.equals("--version")) {
					showVersionInfo();
					System.exit(0);
				}
				else if(optionSymbol.startsWith("--daemon")) {
					if(optionSymbol.equals("--daemon")) {
						new DShellDaemon().waitConnection();	// never return
					}
					else {
						try {
							int port = Integer.parseInt(optionSymbol.split(":")[1]);
							new DShellDaemon(port);
						}
						catch(Exception e) {
							System.err.println("dshell: " + optionSymbol + ": invalid option");
							showHelpAndExit(1, System.err);
						}
					}
				}
				else if(optionSymbol.equals("--debug")) {
					this.debugMode = true;
				}
				else if(optionSymbol.equals("--help")) {
					showHelpAndExit(0, System.out);
				}
				else if(optionSymbol.equals("--logging:stdout")) {
					LoggingContext.getContext().changeAppender(AppenderType.stdout);
				}
				else if(optionSymbol.equals("--logging:stderr")) {
					LoggingContext.getContext().changeAppender(AppenderType.stderr);
				}
				else if(optionSymbol.equals("--logging:syslog")) {
					int nextIndex = i + 1;
					if(nextIndex < args.length && !args[nextIndex].startsWith("-")) {
						LoggingContext.getContext().changeAppender(AppenderType.syslog, args[nextIndex]);
						i++;
					}
					else {
						LoggingContext.getContext().changeAppender(AppenderType.syslog);
					}
				}
				else {
					System.err.println("dshell: " + optionSymbol + ": invalid option");
					showHelpAndExit(1, System.err);
				}
			}
			else if(!foundScriptFile) {
				foundScriptFile = true;
				this.interactiveMode = false;
				this.ARGV = ZenArray.NewZenArray(ZType.StringType);
				this.ARGV.add(optionSymbol);
			}
			else {
				this.ARGV.add(optionSymbol);
			}
		}
	}

	private void execute() {
		ZenEngine engine = loadDShellEngine();
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
					if (this.debugMode && evaledValue != null) {
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
			String scriptName = this.ARGV.get(0);
			//engine.Generator.RootNameSpace.SetSymbol("ARGV", this.ARGV, null);	//FIXME
			boolean status = engine.Load(scriptName);
			engine.Generator.Logger.ShowErrors();
			if(!status) {
				Utils.fatal(1, "abort loading: " + scriptName);
			}
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
		stream.println("    --daemon");
		stream.println("    --daemon:[port]");
		stream.println("    --debug");
		stream.println("    --help");
		stream.println("    --logging:stdout");
		stream.println("    --logging:stderr");
		stream.println("    --logging:syslog [host address]");
		stream.println("    --version");
		System.exit(status);
	}

	private final static ZenEngine loadDShellEngine() {
		ZGenerator generator = new ModifiedAsmGenerator();
		LibZen.ImportGrammar(generator.RootNameSpace, DShellGrammar.class.getName());
		return generator.GetEngine();
	}

	public static void main(String[] args) {
		new DShell(args).execute();
	}
}
