package dshell.lib;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import dshell.exception.DShellException;
import dshell.exception.Errno;

public class ExceptionClassMap {
	public static DShellException createException(String message, String[] causeInfo) {
		// syscall: syscallName: 0, param: 1, errno: 2
		Class<?>[] types = {String.class};
		Object[] args = {message};
		String errnoString = causeInfo[2];
		if(Errno.SUCCESS.match(errnoString)) {
			return DShellException.createNullException(message);
		}
		if(Errno.LAST_ELEMENT.match(errnoString)) {
			return new DShellException(message);
		}
		Class<?> exceptionClass = Errno.getExceptionClass(errnoString);
		try {
			Constructor<?> constructor = exceptionClass.getConstructor(types);
			Errno.DerivedFromErrnoException exception = (Errno.DerivedFromErrnoException) constructor.newInstance(args);
			exception.setSyscallInfo(causeInfo);
			return exception;
		}
		catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		catch (SecurityException e) {
			e.printStackTrace();
		}
		catch (InstantiationException e) {
			e.printStackTrace();
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		Utils.fatal(1, "Creating Exception failed");
		return null;	// unreachable 
	}

	public static void main(String[] args) {
		ArrayList<String> errnoList = Errno.getUnsupportedErrnoList();
		for(String errnoString : errnoList) {
			System.err.println(errnoString + " is not supported");
		}
	}
}
