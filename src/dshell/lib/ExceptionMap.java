package dshell.lib;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.EnumMap;

public class ExceptionMap {
	private final static EnumMap<Errno, Class<?>> exceptMap = new EnumMap<Errno, Class<?>>(Errno.class);

	static {
		verifyAndSetExceptionClass(new ClassListLoader("dshell.exception.errno").getClassList());
	}

	private static void verifyAndSetExceptionClass(ArrayList<Class<?>> exceptionClassList) {
		for(Class<?> exceptionClass : exceptionClassList) {
			Annotation[] anos = exceptionClass.getDeclaredAnnotations();
			if(anos.length == 1 && anos[0] instanceof DerivedFromErrno) {
				Errno key = ((DerivedFromErrno)anos[0]).value();
				if(!exceptMap.containsKey(key)) {
					exceptMap.put(key, exceptionClass);
					continue;
				}
			}
			System.err.println("verification failed: " + exceptionClass.getName());
			System.exit(1);
		}
		// set duplicated errno
		exceptMap.put(Errno.EWOULDBLOCK,getFromMap(Errno.EAGAIN));
		exceptMap.put(Errno.EDEADLOCK, getFromMap(Errno.EDEADLK));
	}

	private static Class<?> getFromMap(Errno key) {
		if(exceptMap.containsKey(key)) {
			return exceptMap.get(key);
		}
		return null;
	}

	public static Class<?> getExceptionClass(int errno) {
		if(errno <= 0 || errno >= Errno.LAST_ELEMENT.ordinal()) {
			return null;
		}
		return getFromMap(Errno.values()[errno]);
	}

	public static Class<?> getExceptionClass(String errnoString) {
		return getFromMap(Errno.valueOf(errnoString));
	}

	public static Class<?> getExceptionClass(Errno key) {
		return getFromMap(key);
	}
}
