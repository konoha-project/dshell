package dshell.lib;

import java.io.File;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Utils {
	public final static boolean isUnixCommand(String cmd) {
		String[] paths = System.getenv("PATH").split(":");
		for(String path : paths) {
			if(isFileExecutable(path + "/" + cmd)) {
				return true;
			}
		}
		return false;
	}

	public final static boolean isFile(String Path) {
		return new File(Path).isFile();
	}

	public final static boolean isDirectory(String Path) {
		return new File(Path).isDirectory();
	}

	public final static boolean isFileExists(String Path) {
		return new File(Path).exists();
	}

	public final static boolean isFileReadable(String Path) {
		return new File(Path).canRead();
	}

	public final static boolean isFileWritable(String Path) {
		return new File(Path).canWrite();
	}

	public final static boolean isFileExecutable(String Path) {
		return new File(Path).canExecute();
	}

	public final static TreeSet<String> getCommandSetFromPath() {
		TreeSet<String> commandSet = new TreeSet<String>();
		String[] paths = System.getenv("PATH").split(":");
		for(String path : paths) {
			if(path.startsWith("~")) {
				path = System.getenv("HOME") + path.substring(1);
			}
			File[] files = new File(path).listFiles();
			if(files == null) {
				continue;
			}
			for(File file : files) {
				if(!file.isDirectory() && file.canExecute()) {
					commandSet.add(file.getName());
				}
			}
		}
		return commandSet;
	}

	public final static void fatal(int status, String message) {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		System.err.println("fatal: " + message);
		for(int i = 2; i < elements.length; i++) {
			StackTraceElement element = elements[i];
			System.err.println("\tat " + element);
		}
		System.exit(status);
	}

	public static boolean matchRegex(String target, String regex) {
		try {
			Pattern pattern = Pattern.compile(regex);
			return pattern.matcher(target).find();
		}
		catch (PatternSyntaxException e) {
		}
		return false;
	}

	public static boolean unmatchRegex(String target, String regex) {
		return !matchRegex(target, regex);
	}

	public static void log(String value) {
		System.out.println(value);
		RuntimeContext.getContext().getLogger().warn(value);
	}
}
