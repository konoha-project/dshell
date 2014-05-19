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
import libbun.util.Var;

import org.objectweb.asm.Type;

public class JavaTypeUtils {
	private final JavaByteCodeGenerator generator;
	private final JavaTypeTable typeTable;

	public JavaTypeUtils(JavaByteCodeGenerator generator, JavaTypeTable typeTable) {
		this.generator = generator;
		this.typeTable = typeTable;
	}

	final Type AsmType(BType zType) {
		Class<?> jClass = this.generator.GetJavaClass(zType, Object.class);
		return Type.getType(jClass);
	}

	final String GetDescripter(BType zType) {
		Class<?> jClass = this.generator.GetJavaClass(zType, null);
		if(jClass != null) {
			return Type.getType(jClass).toString();
		}
		return "L" + zType + ";";
	}

	final String GetTypeDesc(BType zType) {
		Class<?> JClass = this.generator.GetJavaClass(zType);
		return Type.getDescriptor(JClass);
	}

	final String GetMethodDescriptor(BFuncType FuncType) {
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		for(int i = 0; i < FuncType.GetFuncParamSize(); i++) {
			BType ParamType = FuncType.GetFuncParamType(i);
			sb.append(this.GetDescripter(ParamType));
		}
		sb.append(")");
		sb.append(this.GetDescripter(FuncType.GetReturnType()));
		String Desc = sb.toString();
		//		String Desc2 = this.GetMethodDescriptor2(FuncType);
		//		System.out.println(" ** Desc: " + Desc + ", " + Desc2 + ", FuncType: " + FuncType);
		return Desc;
	}

	private boolean MatchParam(Class<?>[] jParams, AbstractListNode ParamList) {
		if(jParams.length != ParamList.GetListSize()) {
			return false;
		}
		for(int j = 0; j < jParams.length; j++) {
			if(jParams[j] == Object.class) {
				continue; // accepting all types
			}
			@Var BType jParamType = this.typeTable.GetBunType(jParams[j]);
			@Var BType ParamType = ParamList.GetListAt(j).Type;
			if(jParamType == ParamType || jParamType.Accept(ParamList.GetListAt(j).Type)) {
				continue;
			}
			if(jParamType.IsFloatType() && ParamType.IsIntType()) {
				continue;
			}
			if(jParamType.IsIntType() && ParamType.IsFloatType()) {
				continue;
			}
			return false;
		}
		return true;
	}

	protected Constructor<?> GetConstructor(BType RecvType, AbstractListNode ParamList) {
		Class<?> NativeClass = this.generator.GetJavaClass(RecvType);
		if(NativeClass != null) {
			try {
				Constructor<?>[] Methods = NativeClass.getConstructors();
				for(int i = 0; i < Methods.length; i++) {
					@Var Constructor<?> jMethod = Methods[i];
					if(!Modifier.isPublic(jMethod.getModifiers())) {
						continue;
					}
					if(this.MatchParam(jMethod.getParameterTypes(), ParamList)) {
						return jMethod;
					}
				}
			} catch (SecurityException e) {
			}
		}
		return null;
	}

	protected Method GetMethod(BType RecvType, String MethodName, AbstractListNode ParamList) {
		Class<?> NativeClass = this.generator.GetJavaClass(RecvType);
		if(NativeClass != null) {
			try {
				Method[] Methods = NativeClass.getMethods();
				for(int i = 0; i < Methods.length; i++) {
					@Var Method jMethod = Methods[i];
					if(!MethodName.equals(jMethod.getName())) {
						continue;
					}
					if(!Modifier.isPublic(jMethod.getModifiers())) {
						continue;
					}
					if(this.MatchParam(jMethod.getParameterTypes(), ParamList)) {
						return jMethod;
					}
				}
			} catch (SecurityException e) {
			}
		}
		return null;
	}

	Type AsmType(Class<?> jClass) {
		return Type.getType(jClass);
	}

	Class<?> AsArrayClass(BType zType) {
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

	Class<?> AsElementClass(BType zType) {
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

	String NewArrayDescriptor(BType ArrayType) {
		BType zParamType = ArrayType.GetParamType(0);
		if(zParamType.IsBooleanType()) {
			return Type.getMethodDescriptor(AsmType(void.class), new Type[] {this.AsmType(int.class), this.AsmType(boolean[].class)});
		}
		if(zParamType.IsIntType()) {
			return Type.getMethodDescriptor(AsmType(void.class), new Type[] {this.AsmType(int.class), this.AsmType(long[].class)});
		}
		if(zParamType.IsFloatType()) {
			return Type.getMethodDescriptor(AsmType(void.class), new Type[] {this.AsmType(int.class), this.AsmType(double[].class)});
		}
		return Type.getMethodDescriptor(AsmType(void.class), new Type[] {this.AsmType(int.class), this.AsmType(Object[].class)});
	}
}
