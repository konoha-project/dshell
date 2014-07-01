package dshell.internal.parser;

import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import dshell.internal.type.DSType.PrimitiveType;
import dshell.internal.type.TypePool;
import dshell.internal.type.DSType;

public class TypeUtils {
	public static boolean matchParamsType(final DSType[] paramTypes, final DSType[] givenTypes) {
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
	 * create type descriptor from type.
	 * @param type
	 * @return
	 */
	public static Type toTypeDescriptor(DSType type) {
		return toTypeDescriptor(type.getInternalName());
	}

	/**
	 * create type descriptor from internal class name.
	 * @param internalName
	 * fully qualified class name.
	 * @return
	 */
	public static Type toTypeDescriptor(String internalName) {
		if(internalName.equals("long")) {
			return Type.LONG_TYPE;
		} else if(internalName.equals("double")) {
			return Type.DOUBLE_TYPE;
		} else if(internalName.equals("boolean")) {
			return Type.BOOLEAN_TYPE;
		} else if(internalName.equals("void")) {
			return Type.VOID_TYPE;
		} else {
			return Type.getType( "L" + internalName + ";");
		}
	}

	/**
	 * create mrthod descriptor from types.
	 * @param returnType
	 * @param methodName
	 * @param paramTypeList
	 * @return
	 */
	public static Method toMehtodDescriptor(DSType returnType, String methodName, List<DSType> paramTypeList) {
		int size = paramTypeList.size();
		Type[] paramtypeDecs = new Type[size];
		for(int i = 0; i < size; i++) {
			paramtypeDecs[i] = toTypeDescriptor(paramTypeList.get(i));
		}
		Type returnTypeDesc = toTypeDescriptor(returnType);
		return new Method(methodName, returnTypeDesc, paramtypeDecs);
	}

	public static Method toConstructorDescriptor(List<DSType> paramTypeList) {
		return toMehtodDescriptor(TypePool.voidType, "<init>", paramTypeList);
	}

	public static Method toArrayConstructorDescriptor(DSType elementType) {
		Type returnTypeDesc = Type.VOID_TYPE;
		Type elementTypeDesc = toTypeDescriptor(elementType);
		if(!(elementType instanceof PrimitiveType)) {
			elementTypeDesc = Type.getType(Object.class);
		}
		Type paramTypeDesc = Type.getType("[" + elementTypeDesc.getDescriptor());
		Type[] paramtypeDecs = new Type[]{paramTypeDesc};
		return new Method("<init>", returnTypeDesc, paramtypeDecs);
	}

	public static Method toMapConstructorDescriptor() {
		Type returnTypeDesc = Type.VOID_TYPE;
		Type keysTypeDesc = Type.getType("[Ljava/lang/String;");
		Type valuesTypeDesc = Type.getType("[Ljava/lang/Object;");
		Type[] paramTypeDescs = new Type[] {keysTypeDesc, valuesTypeDesc};
		return new Method("<init>", returnTypeDesc, paramTypeDescs);
	}
} 
