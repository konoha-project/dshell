package dshell.internal.jvm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import libbun.ast.AbstractListNode;
import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.util.BArray;
import libbun.util.BBooleanArray;
import libbun.util.BFloatArray;
import libbun.util.BIntArray;

import org.objectweb.asm.Type;

public class JavaTypeUtils {
	private final JavaByteCodeGenerator generator;
	private final JavaTypeTable typeTable;

	public JavaTypeUtils(JavaByteCodeGenerator generator, JavaTypeTable typeTable) {
		this.generator = generator;
		this.typeTable = typeTable;
	}

	final Type asmType(BType zType) {
		Class<?> jClass = this.generator.getJavaClass(zType, Object.class);
		return Type.getType(jClass);
	}

	final String getDescripter(BType zType) {
		Class<?> jClass = this.generator.getJavaClass(zType, null);
		if(jClass != null) {
			return Type.getType(jClass).toString();
		}
		return "L" + zType + ";";
	}

	final String getTypeDesc(BType zType) {
		Class<?> jClass = this.generator.getJavaClass(zType);
		return Type.getDescriptor(jClass);
	}

	final String getMethodDescriptor(BFuncType funcType) {
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		for(int i = 0; i < funcType.GetFuncParamSize(); i++) {
			BType ParamType = funcType.GetFuncParamType(i);
			sb.append(this.getDescripter(ParamType));
		}
		sb.append(")");
		sb.append(this.getDescripter(funcType.GetReturnType()));
		String desc = sb.toString();
		//		String Desc2 = this.GetMethodDescriptor2(FuncType);
		//		System.out.println(" ** Desc: " + Desc + ", " + Desc2 + ", FuncType: " + FuncType);
		return desc;
	}

	private boolean matchParam(Class<?>[] jParams, AbstractListNode paramList) {
		if(jParams.length != paramList.GetListSize()) {
			return false;
		}
		for(int j = 0; j < jParams.length; j++) {
			if(jParams[j] == Object.class) {
				continue; // accepting all types
			}
			BType jParamType = this.typeTable.GetBunType(jParams[j]);
			BType paramType = paramList.GetListAt(j).Type;
			if(jParamType == paramType || jParamType.Accept(paramList.GetListAt(j).Type)) {
				continue;
			}
			if(jParamType.IsFloatType() && paramType.IsIntType()) {
				continue;
			}
			if(jParamType.IsIntType() && paramType.IsFloatType()) {
				continue;
			}
			return false;
		}
		return true;
	}

	protected Constructor<?> getConstructor(BType recvType, AbstractListNode paramList) {
		Class<?> javaClass = this.generator.getJavaClass(recvType);
		if(javaClass != null) {
			try {
				Constructor<?>[] Methods = javaClass.getConstructors();
				for(int i = 0; i < Methods.length; i++) {
					Constructor<?> jMethod = Methods[i];
					if(!Modifier.isPublic(jMethod.getModifiers())) {
						continue;
					}
					if(this.matchParam(jMethod.getParameterTypes(), paramList)) {
						return jMethod;
					}
				}
			} catch (SecurityException e) {
			}
		}
		return null;
	}

	protected Method getMethod(BType recvType, String methodName, AbstractListNode paramList) {
		Class<?> javaClass = this.generator.getJavaClass(recvType);
		if(javaClass != null) {
			try {
				Method[] methods = javaClass.getMethods();
				for(int i = 0; i < methods.length; i++) {
					Method jMethod = methods[i];
					if(!methodName.equals(jMethod.getName())) {
						continue;
					}
					if(!Modifier.isPublic(jMethod.getModifiers())) {
						continue;
					}
					if(this.matchParam(jMethod.getParameterTypes(), paramList)) {
						return jMethod;
					}
				}
			} catch (SecurityException e) {
			}
		}
		return null;
	}

	Type asmType(Class<?> jClass) {
		return Type.getType(jClass);
	}

	Class<?> asArrayClass(BType zType) {
		BType zParamType = zType.GetParamType(0);
		if(zParamType.IsBooleanType()) {
			return BBooleanArray.class;
		}
		if(zParamType.IsIntType()) {
			return BIntArray.class;
		}
		if(zParamType.IsFloatType()) {
			return BFloatArray.class;
		}
		return BArray.class;
	}

	Class<?> asElementClass(BType zType) {
		BType zParamType = zType.GetParamType(0);
		if(zParamType.IsBooleanType()) {
			return boolean.class;
		}
		if(zParamType.IsIntType()) {
			return long.class;
		}
		if(zParamType.IsFloatType()) {
			return double.class;
		}
		return Object.class;
	}

	String newArrayDescriptor(BType arrayType) {
		BType zParamType = arrayType.GetParamType(0);
		if(zParamType.IsBooleanType()) {
			return Type.getMethodDescriptor(asmType(void.class), new Type[] {this.asmType(int.class), this.asmType(boolean[].class)});
		}
		if(zParamType.IsIntType()) {
			return Type.getMethodDescriptor(asmType(void.class), new Type[] {this.asmType(int.class), this.asmType(long[].class)});
		}
		if(zParamType.IsFloatType()) {
			return Type.getMethodDescriptor(asmType(void.class), new Type[] {this.asmType(int.class), this.asmType(double[].class)});
		}
		return Type.getMethodDescriptor(asmType(void.class), new Type[] {this.asmType(int.class), this.asmType(Object[].class)});
	}
}
