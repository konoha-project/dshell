package dshell.lib;

import java.io.File;
import java.lang.reflect.Field;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Utils {
	public final static String getCommandFromPath(String cmd) {
		String[] paths = getEnv("PATH").split(":");
		for(String path : paths) {
			String fullPath = resolveHome(path + "/" + cmd);
			if(isFileExecutable(fullPath)) {
				return fullPath;
			}
		}
		return null;
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
		String[] paths = getEnv("PATH").split(":");
		for(String path : paths) {
			path = resolveHome(path);
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

	public static String getEnv(String key) {
		String env = RuntimeContext.getContext().getenv(key);
		return env == null ? "" : env;
	}

	public static String setEnv(String key, String env) {
		int ret = RuntimeContext.getContext().setenv(key, env, true);
		return ret == 0 ? env : "";
	}

	public static void setValue(Object targetObject, String fieldName, Object value) {
		try {
			Field field = targetObject.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(targetObject, value);
			field.setAccessible(false);
		}
		catch (Exception e) {
			e.printStackTrace();
			Utils.fatal(1, "field access failed: " + fieldName);
		}
	}

	public static Object getValue(Object targetObject, String fieldName) {
		try {
			Field field = targetObject.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			Object value = field.get(targetObject);
			field.setAccessible(false);
			return value;
		}
		catch (Exception e) {
			e.printStackTrace();
			Utils.fatal(1, "field access failed: " + fieldName);
		}
		return null;
	}

	public static String resolveHome(String Path) {
		if(Path.equals("~")) {
			return Utils.getEnv("HOME");
		}
		else if(Path.startsWith("~/")) {
			return Utils.getEnv("HOME") + Path.substring(1);
		}
		return Path;
	}

	private final static Pattern defaultDelimPattern = Pattern.compile("[\n\t ]+", Pattern.UNIX_LINES);
	public static String[] splitWithDelim(String targetValue) {	//TODO: support IFS variable
		return defaultDelimPattern.matcher(targetValue).replaceAll(" ").split(" ");
	}

	public static void assertDShell(boolean result) {
		if(!result) {
			new AssertionError("").printStackTrace();
			System.exit(1);
		}
	}

	public static class AssertionError extends dshell.exception.Exception {
		private static final long serialVersionUID = 1160350956527375486L;
		public AssertionError(String message) {
			super(message);
		}
	}

	public static long stringToLong(String value) {
		return Long.parseLong(value);
	}

	public static double stringToDouble(String value) {
		return Double.parseDouble(value);
	}

	public static String removeNewLine(String Value) {
		int Size = Value.length();
		int EndIndex = Size;
		for(int i = Size - 1; i > -1; i--) {
			char ch = Value.charAt(i);
			if(ch != '\n') {
				EndIndex = i + 1;
				break;
			}
		}
		return EndIndex == Size ? Value : Value.substring(0, EndIndex);
	}
}
