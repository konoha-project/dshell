package dshell.internal.lib;

import java.io.File;
import java.lang.reflect.Field;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Utils {
	public final static String getCommandFromPath(String cmd) {
		if(cmd.equals("")) {
			return null;
		}
		String[] paths = getEnv("PATH").split(":");
		for(String path : paths) {
			String fullPath = resolveHome(path + "/" + cmd);
			if(isFileExecutable(fullPath)) {
				return fullPath;
			}
		}
		return null;
	}

	public final static boolean isFile(String path) {
		return new File(path).isFile();
	}

	public final static boolean isDirectory(String path) {
		return new File(path).isDirectory();
	}

	public final static boolean isFileExists(String path) {
		return new File(path).exists();
	}

	public final static boolean isFileReadable(String path) {
		return new File(path).canRead();
	}

	public final static boolean isFileWritable(String path) {
		return new File(path).canWrite();
	}

	public final static boolean isFileExecutable(String path) {
		return new File(path).canExecute();
	}

	public final static TreeSet<String> getCommandSetFromPath() {
		return getCommandSetFromPath(false);
	}

	public final static TreeSet<String> getCommandSetFromPath(boolean requireFullPath) {
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
					String fileName = file.getName();
					if(requireFullPath) {
						fileName = path + "/" + fileName;
					}
					commandSet.add(fileName);
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

	public static String resolveHome(String path) {
		if(path.equals("~")) {
			return Utils.getEnv("HOME");
		}
		else if(path.startsWith("~/")) {
			return Utils.getEnv("HOME") + path.substring(1);
		}
		return path;
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

	public static class AssertionError extends dshell.internal.exception.Exception {
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

	public static String removeNewLine(String value) {
		int size = value.length();
		int endIndex = size;
		for(int i = size - 1; i > -1; i--) {
			char ch = value.charAt(i);
			if(ch != '\n') {
				endIndex = i + 1;
				break;
			}
		}
		return endIndex == size ? value : value.substring(0, endIndex);
	}

	public static String getUserName() {
		return getEnv("USER");
	}
}
