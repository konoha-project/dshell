package dshell.internal.parser;

import java.util.HashMap;

import dshell.lang.GenericArray;
import dshell.lang.GenericMap;
import dshell.lang.PrimitiveArray;
import dshell.lang.proxy.BooleanProxy;
import dshell.lang.proxy.FloatProxy;
import dshell.lang.proxy.IntProxy;
import dshell.lang.proxy.StringProxy;

/**
 * It contains builtin types ant user defined types.
 * @author skgchxngsxyz-osx
 *
 */
public class TypePool {
	public final Type intType;
	public final Type floatType;
	public final Type booleanType;
	public final Type stringType;
	public final Type exceptionType;
	public final Type voidType;

	/**
	 * Use Type Checker
	 */
	public final Type unresolvedType = new UnresolvedType();
	protected final Type baseArrayType;
	protected final Type baseMapType;

	/**
	 * type name to type translation table
	 */
	protected final HashMap<String, Type> typeMap;

	/**
	 * native class to type translation table
	 */
	protected final HashMap<Class<?>, Type> class2TypeMap;

	protected TypePool() {
		this.typeMap = new HashMap<>();
		this.class2TypeMap = new HashMap<>();
		this.intType = this.setType("int", new PrimitiveType("int", long.class));
		this.floatType = this.setType("float", new PrimitiveType("float", double.class));
		this.booleanType = this.setType("boolean", new PrimitiveType("boolean", boolean.class));
		this.stringType = this.setType("String", new StringType());
		this.exceptionType = this.setType("Exception", new Type("Exception", dshell.lang.Exception.class));
		this.voidType = this.setType("void", new VoidType());
		this.baseArrayType = this.setType("Array", new GenericBaseType("Array", GenericArray.class));
		this.baseMapType = this.setType("Map", new GenericBaseType("Map", GenericMap.class));
	}

	/**
	 * set type.
	 * @param typeName
	 * - type name
	 * @param type
	 * - target type
	 * @return
	 * - If not contains type, return type. if contains type, return unresolved type.
	 */
	private Type setType(String typeName, Type type) {
		if(this.typeMap.containsKey(typeName)) {
			return this.unresolvedType;
		}
		this.typeMap.put(typeName, type);
		this.class2TypeMap.put(type.getNativeClass(), type);
		return type;
	}
	

	public static String toGenericTypeName(GenericBaseType baseType, Type type) {
		return toGenericTypeName(baseType, new Type[]{type});
	}

	public static String toGenericTypeName(GenericBaseType baseType, Type[] types) {
		StringBuilder sBuilder = new StringBuilder();
		if(baseType.equals(TypePool.getInstance().baseArrayType)) {
			sBuilder.append(types[0].toString());
			sBuilder.append("[]");
		}
		else {
			sBuilder.append(baseType.toString());
			sBuilder.append("<");
			sBuilder.append(types[0].toString());
			int size = types.length;
			for(int i = 1; i < size; i++) {
				sBuilder.append(",");
				sBuilder.append(types[i].toString());
			}
			sBuilder.append(">");
		}
		return sBuilder.toString();
	}

	public static String toFuncTypeName(Type returnType, Type[] paramsType) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("func<");
		sBuilder.append(returnType.toString());
		for(Type paramType : paramsType) {
			sBuilder.append(",");
			sBuilder.append(paramType.toString());
		}
		sBuilder.append(">");
		return sBuilder.toString();
	}

	/**
	 * Represents dshell type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class Type {
		/**
		 * String representation of this type.
		 * Must be unique name.
		 */
		private final String typeName;

		/**
		 * Corresponding java class.
		 */
		private final Class<?> nativeClass;

		protected Type(String typeName, Class<?> nativeClass) {
			this.typeName = typeName;
			this.nativeClass = nativeClass;
		}

		public Class<?> getNativeClass() {
			return this.nativeClass;
		}

		public String getTypeName() {
			return this.typeName;
		}

		@Override
		public String toString() {
			return this.typeName;
		}
	}

	/**
	 * Behave like a method call proxy.
	 * When method calling, call static method of proxy class instead of instance method.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static interface NativeClassProxy {
		public Class<?> getProxyClass();
	}

	/**
	 * Represents primitive type (int, float, boolean type).
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class PrimitiveType extends Type implements NativeClassProxy {
		private final Class<?> wrapperClass;
		private final Class<?> proxyClass;

		private PrimitiveType(String typeName, Class<?> nativeClass) {
			super(typeName, nativeClass);
			if(nativeClass.equals(long.class)) {
				this.wrapperClass = Long.class;
				this.proxyClass = IntProxy.class;
			} else if(nativeClass.equals(double.class)) {
				this.wrapperClass = Double.class;
				this.proxyClass = FloatProxy.class;
			} else if(nativeClass.equals(boolean.class)) {
				this.wrapperClass = Boolean.class;
				this.proxyClass = BooleanProxy.class;
			} else {
				throw new RuntimeException(nativeClass.getCanonicalName() + " is unsupported class");
			}
		}

		/**
		 * get primitive wrapper class
		 * @return
		 */
		public Class<?> getWrapperClass() {
			return this.wrapperClass;
		}

		/**
		 * get method call proxy class
		 */
		@Override
		public Class<?> getProxyClass() {
			return this.proxyClass;
		}
	}

	/**
	 * Represent String type
	 * It contains StringProxy class.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private final static class StringType extends Type implements NativeClassProxy {
		private final Class<?> proxyClass;

		protected StringType() {
			super("String", String.class);
			this.proxyClass = StringProxy.class;
		}

		@Override
		public Class<?> getProxyClass() {
			return this.proxyClass;
		}
	}

	/**
	 * Represent void type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class VoidType extends Type {
		private VoidType() {
			super("void", Void.class);
		}
	}

	/**
	 * Represents generic type (array type or map type).
	 * It contains elements type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class GenericType extends Type {
		private final String actualTypeName;
		private final GenericBaseType baseType;
		private final Type[] elementsType;

		protected GenericType(GenericBaseType baseType, Type[] elementsType) {
			super("", null);
			this.baseType = baseType;
			this.elementsType = elementsType;
			this.actualTypeName = TypePool.toGenericTypeName(this.baseType, this.elementsType);
		}

		@Override
		public Class<?> getNativeClass() {
			return this.baseType.getNativeClass();
		}

		@Override
		public String getTypeName() {
			return this.actualTypeName;
		}

		@Override
		public String toString() {
			return this.actualTypeName;
		}
	}

	/**
	 * Represents base type of generic type.
	 * Do not use it directly.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class GenericBaseType extends Type {
		private GenericBaseType(String typeName, Class<?> nativeClass) {
			super(typeName, nativeClass);
		}
	}

	/**
	 * Represents generic array type.
	 * It contains element type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class GenericArrayType extends GenericType {
		private GenericArrayType(Type elementType) {
			super((GenericBaseType)TypePool.getInstance().baseArrayType, new Type[]{elementType});
		}
	}

	public final static class PrimitiveArrayType extends GenericArrayType {
		private final Class<?> primitiveArrayClass;

		private PrimitiveArrayType(PrimitiveType elementType) {
			super(elementType);
			if(elementType.equals(TypePool.getInstance().intType)) {
				this.primitiveArrayClass = PrimitiveArray.IntArray.class;
			} else if(elementType.equals(TypePool.getInstance().floatType)) {
				this.primitiveArrayClass = PrimitiveArray.FloatArray.class;
			} else if(elementType.equals(TypePool.getInstance().booleanType)) {
				this.primitiveArrayClass = PrimitiveArray.BooleanArray.class;
			} else {
				throw new RuntimeException(elementType.getNativeClass().getCanonicalName() + " is unsupported class");
			}
		}

		@Override
		public Class<?> getNativeClass() {
			return this.primitiveArrayClass;
		}
	}

	/**
	 * Represents generic map type.
	 * It contains element type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public final static class GenericMapType extends GenericType {
		private GenericMapType(Type elementType) {
			super((GenericBaseType)TypePool.getInstance().baseMapType, new Type[]{elementType});
		}
	}

	/**
	 * Represents function type.
	 * It contains parameters type and return type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FunctionType extends Type {	//TODO:
		private final String funcTypeName;
		private final Type returnType;
		private final Type[] paramsType;
//		private final Class<?> funcClass;
		
		private FunctionType(Type returnType, Type[] paramsType) {
			super("", null);
			this.returnType = returnType;
			this.paramsType = paramsType;
			this.funcTypeName = TypePool.toFuncTypeName(this.returnType, this.paramsType);
		}

		@Override
		public Class<?> getNativeClass() {
			return null;	//TODO:
		}

		@Override
		public String getTypeName() {
			return this.funcTypeName;
		}

		public Type getReturnType() {
			return this.returnType;
		}

		public Type getParamTypeAt(int index) {
			return this.paramsType[index];
		}

		public Type[] getParamsType() {
			return this.paramsType;
		}

		@Override
		public String toString() {
			return this.funcTypeName;
		}
	}

	/**
	 * Represents class type.
	 * Class name must be upper case.
	 * It contains super class type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public final static class ClassType extends Type {
		private final Type superType;
		private Class<?> generatedClass;

		private ClassType(String className, Type superType) {
			super(className, null);
			this.superType = superType;
		}

		public Type getSuperType() {
			return this.superType;
		}

		/**
		 * set generated class.
		 * @param generatedClass
		 */
		private void setGeneratedClass(Class<?> generatedClass) {
			this.generatedClass = generatedClass;
		}

		@Override
		public Class<?> getNativeClass() {
			return this.generatedClass;
		}
	}

	/**
	 * It is an initial value of expression node.
	 * Type checker replaces this type to actual type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public final static class UnresolvedType extends Type {
		private UnresolvedType(){
			super("$unresolved$", null);
		}

		@Override
		public Class<?> getNativeClass() {
			throw new RuntimeException("cannot call this method");
		}
	}

	// holder class
	private static class Holder {
		private final static TypePool INSTANCE = new TypePool();
	}

	/**
	 * get type pool instance
	 * @return
	 */
	public static TypePool getInstance() {
		return Holder.INSTANCE;
	}
}
