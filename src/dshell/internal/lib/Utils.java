package dshell.internal.lib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.TreeSet;
import java.util.regex.Pattern;

import dshell.lang.NativeException;

/**
 * some utilities.
 * @author skgchxngsxyz-opensuse
 *
 */
public class Utils {
	/**
	 * get full path of command.
	 * @param cmd
	 * @return
	 * - return null, if has no executable command.
	 */
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

	/**
	 * print meesage and stack trace before exit.
	 * @param status
	 * @param message
	 */
	public final static void fatal(int status, String message) {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		System.err.println("fatal: " + message);
		for(int i = 2; i < elements.length; i++) {
			StackTraceElement element = elements[i];
			System.err.println("\tat " + element);
		}
		System.exit(status);
	}

	public static void log(String value) {
		System.out.println(value);
		RuntimeContext.getInstance().getLogger().warn(value);
	}

	/**
	 * get environmental variable.
	 * @param key
	 * @return
	 * - if has no env, return empty string.
	 */
	public static String getEnv(String key) {
		String env = RuntimeContext.getInstance().getenv(key);
		return env == null ? "" : env;
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

	/**
	 * replace ~ to home dir path.
	 * @param path
	 * - no null string
	 * @return
	 */
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

	public static void printException(InvocationTargetException e) {
		NativeException.wrapException(e.getCause()).printStackTrace();
	}

	public static void appendStringifiedValue(StringBuilder sb, Object value) {
		if(value == null) {
			sb.append("$null$");
		}
		if(value instanceof String) {
			sb.append('"');
			sb.append(value.toString());
			sb.append('"');
		} else {
			sb.append(value.toString());
		}
	}

	public static String readFromFile(String fileName, boolean forceExit) {
		try(FileInputStream input = new FileInputStream(fileName)){
			ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[512];
			int readSize = 0;
			while((readSize = input.read(buffer, 0, buffer.length)) > -1) {
				bufferStream.write(buffer, 0, readSize);
			}
			return bufferStream.toString();
		} catch (IOException e) {
			if(forceExit) {
				System.err.println(e.getMessage());
				Utils.fatal(1, "cannot read file: " + fileName);
			}
		}
		return null;
	}
}
