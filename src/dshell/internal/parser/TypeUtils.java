package dshell.internal.parser;

import java.lang.reflect.Constructor;

import dshell.internal.parser.TypePool.Type;

public class TypeUtils {
	public static boolean matchParamsType(final Type[] paramTypes, final Type[] givenTypes) {
		assert paramTypes != null;
		assert givenTypes != null;
		if(paramTypes.length != givenTypes.length) {
			return false;
		}
		int size = paramTypes.length;
		for(int i = 0; i < size; i++) {
			if(!paramTypes[i].isAssignableFrom(givenTypes[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * get java public method (static method or instance method) by reflection.
	 * @param ownerClass
	 * - method owner class.
	 * @param methodName
	 * - target method name.
	 * @param paramClasses
	 * - method parameter classes (not contain receiver class).
	 * may be null if has not parameters.
	 * @return
	 */
	public static java.lang.reflect.Method getMethod(Class<?> ownerClass, String methodName, Class<?>[] paramClasses) {
		try {
			return ownerClass.getMethod(methodName, paramClasses);
		} catch(Throwable t) {
			t.printStackTrace();
			throw new RuntimeException(t);
		}
	}

	/**
	 * get java public constructor
	 * @param ownerClass
	 * @param paramClasses
	 * - method parameter classes (not contain receiver class).
	 * may be null if has not parameters.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> java.lang.reflect.Constructor<T> getConstructor(Class<?> ownerClass, Class<?>[] paramClasses) {
		try {
			return (Constructor<T>) ownerClass.getConstructor(paramClasses);
		} catch(Throwable t) {
			t.printStackTrace();
			throw new RuntimeException(t);
		}
	}
} 
