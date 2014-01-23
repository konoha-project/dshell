package dshell.lib;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumMap;

import dshell.exception.DShellException;
import dshell.exception.RelatedSyscallException;
import dshell.exception.UnimplementedErrnoException;
import dshell.util.Utils;

public class ExceptionClassMap {
	private final static EnumMap<Errno, Class<?>> exceptMap = new EnumMap<Errno, Class<?>>(Errno.class);

	static {
		System.err.println("init ExceptionMap");
		verifyAndSetExceptionClass(new ClassListLoader("dshell.exception.errno").getClassList());
	}

	private static void verifyAndSetExceptionClass(ArrayList<Class<?>> exceptionClassList) {
		System.err.println(exceptionClassList.size());
		for(Class<?> exceptionClass : exceptionClassList) {
			Annotation[] anos = exceptionClass.getDeclaredAnnotations();
			if(anos.length == 1 && anos[0] instanceof DerivedFromErrno) {
				Errno key = ((DerivedFromErrno)anos[0]).value();
				if(!exceptMap.containsKey(key)) {
					exceptMap.put(key, exceptionClass);
					continue;
				}
			}
			Utils.fatal(1, "verification failed: " + exceptionClass.getName());
		}
		// set duplicated errno
		exceptMap.put(Errno.EWOULDBLOCK,getFromMap(Errno.EAGAIN));
		exceptMap.put(Errno.EDEADLOCK, getFromMap(Errno.EDEADLK));
	}

	private static Class<?> getFromMap(Errno key) {
		if(key == Errno.SUCCESS || key == Errno.LAST_ELEMENT) {
			Utils.fatal(1, "inavlid errno: " + key.name());
		}
		if(exceptMap.containsKey(key)) {
			Class<?> exceptionClass = exceptMap.get(key);
			if(exceptionClass != null) {
				return exceptMap.get(key);
			}
		}
		return UnimplementedErrnoException.class;
	}

	public static Class<?> getExceptionClass(int errno) {
		return getFromMap(Errno.toErrrno(errno));
	}

	public static Class<?> getExceptionClass(String errnoString) {
		return getFromMap(Errno.valueOf(errnoString));
	}

	public static Class<?> getExceptionClass(Errno key) {
		return getFromMap(key);
	}

	public static DShellException createException(String message, String[] causeInfo) {
		// syscall: syscallName: 0, param: 1, errno: 2
		Class<?>[] types = {String.class, String.class, String[].class};
		Object[] args = {message, message, causeInfo};
		String errnoString = causeInfo[2];
		if(Errno.SUCCESS.match(errnoString)) {
			return null;
		}
		if(Errno.LAST_ELEMENT.match(errnoString)) {
			return new DShellException(message);
		}
		Class<?> exceptionClass = getExceptionClass(errnoString);
		try {
			Constructor<?> constructor = exceptionClass.getConstructor(types);
			return (RelatedSyscallException) constructor.newInstance(args);
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
		return null;
	}
}
