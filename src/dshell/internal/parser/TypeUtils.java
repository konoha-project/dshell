package dshell.internal.parser;

import java.lang.reflect.Constructor;
import java.util.List;

import dshell.internal.parser.TypePool.PrimitiveType;
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

	/**
	 * create type descriptor from type.
	 * @param type
	 * @return
	 */
	public static org.objectweb.asm.Type toTypeDescriptor(Type type) {
		return toTypeDescriptor(type.getInternalName());
	}

	/**
	 * create type descriptor from internal class name.
	 * @param internalName
	 * fully qualified class name.
	 * @return
	 */
	public static org.objectweb.asm.Type toTypeDescriptor(String internalName) {
		if(internalName.equals("long")) {
			return org.objectweb.asm.Type.LONG_TYPE;
		} else if(internalName.equals("double")) {
			return org.objectweb.asm.Type.DOUBLE_TYPE;
		} else if(internalName.equals("boolean")) {
			return org.objectweb.asm.Type.BOOLEAN_TYPE;
		} else if(internalName.equals("void")) {
			return org.objectweb.asm.Type.VOID_TYPE;
		} else {
			return org.objectweb.asm.Type.getType( "L" + internalName + ";");
		}
	}

	/**
	 * create mrthod descriptor from types.
	 * @param returnType
	 * @param methodName
	 * @param paramTypeList
	 * @return
	 */
	public static org.objectweb.asm.commons.Method toMehtodDescriptor(Type returnType, String methodName, List<Type> paramTypeList) {
		int size = paramTypeList.size();
		org.objectweb.asm.Type[] paramtypeDecs = new org.objectweb.asm.Type[size];
		for(int i = 0; i < size; i++) {
			paramtypeDecs[i] = toTypeDescriptor(paramTypeList.get(i));
		}
		org.objectweb.asm.Type returnTypeDesc = toTypeDescriptor(returnType);
		return new org.objectweb.asm.commons.Method(methodName, returnTypeDesc, paramtypeDecs);
	}

	public static org.objectweb.asm.commons.Method toConstructorDescriptor(List<Type> paramTypeList) {
		return toMehtodDescriptor(new TypePool.VoidType(), "<init>", paramTypeList);
	}

	public static org.objectweb.asm.commons.Method toArrayConstructorDescriptor(Type elementType) {
		org.objectweb.asm.Type returnTypeDesc = org.objectweb.asm.Type.VOID_TYPE;
		org.objectweb.asm.Type elementTypeDesc = toTypeDescriptor(elementType);
		if(!(elementType instanceof PrimitiveType)) {
			elementTypeDesc = org.objectweb.asm.Type.getType(Object.class);
		}
		org.objectweb.asm.Type paramTypeDesc = org.objectweb.asm.Type.getType("[" + elementTypeDesc.getDescriptor());
		org.objectweb.asm.Type[] paramtypeDecs = new org.objectweb.asm.Type[]{paramTypeDesc};
		return new org.objectweb.asm.commons.Method("<init>", returnTypeDesc, paramtypeDecs);
	}
} 
