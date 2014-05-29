package dshell.internal.parser;

import java.util.HashMap;

import dshell.lang.BooleanArray;
import dshell.lang.FloatArray;
import dshell.lang.GenericArray;
import dshell.lang.GenericMap;
import dshell.lang.IntArray;
import dshell.lang.proxy.BooleanProxy;
import dshell.lang.proxy.FloatProxy;
import dshell.lang.proxy.IntProxy;

/**
 * It contains builtin types ant user defined types.
 * @author skgchxngsxyz-osx
 *
 */
public class TypePool {
	/**
	 * Equivalent to java long.
	 */
	public final Type intType;

	/**
	 * Equivalent to java double.
	 */
	public final Type floatType;

	/**
	 * Equivalent to java boolean.
	 */
	public final Type booleanType;

	/**
	 * Represents D-Shell string type.
	 */
	public final Type stringType;

	/**
	 * Represents D-Shell root excepion type.
	 */
	public final Type exceptionType;

	/**
	 * Represents D-Shell root class type.
	 * It is equivalent to java Object.
	 */
	public final Type objectType;

	/**
	 * Equivalent to java void.
	 */
	public final Type voidType;

	/**
	 * Used by Type Checker
	 */
	public final Type unresolvedType = new UnresolvedType();
	public final Type baseArrayType;
	public final Type baseMapType;

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
		/**
		 * add basic type.
		 */
		this.intType       = this.setTypeAndThrowIfDefined(new PrimitiveType("int", long.class));
		this.floatType     = this.setTypeAndThrowIfDefined(new PrimitiveType("float", double.class));
		this.booleanType   = this.setTypeAndThrowIfDefined(new PrimitiveType("boolean", boolean.class));
		this.stringType    = this.setTypeAndThrowIfDefined(new Type("String", dshell.lang.DShellString.class));
		this.exceptionType = this.setTypeAndThrowIfDefined(new Type("Exception", dshell.lang.Exception.class));
		this.objectType    = this.setTypeAndThrowIfDefined(new Type("Object", Object.class));
		this.voidType      = this.setTypeAndThrowIfDefined(new VoidType());
		this.baseArrayType = this.setTypeAndThrowIfDefined(new GenericBaseType("Array", GenericArray.class));
		this.baseMapType   = this.setTypeAndThrowIfDefined(new GenericBaseType("Map", GenericMap.class));

		/**
		 * add primitive array type.
		 */
		this.setTypeAndThrowIfDefined(new PrimitiveArrayType((GenericBaseType) this.baseArrayType, (PrimitiveType) this.intType));
		this.setTypeAndThrowIfDefined(new PrimitiveArrayType((GenericBaseType) this.baseArrayType, (PrimitiveType) this.floatType));
		this.setTypeAndThrowIfDefined(new PrimitiveArrayType((GenericBaseType) this.baseArrayType, (PrimitiveType) this.booleanType));
	}

	/**
	 * set type and native class to typeMap.
	 * @param type
	 * @return
	 */
	private Type setTypeAndThrowIfDefined(Type type) {
		return this.setTypeAndThrowIfDefined(type, false);
	}

	/**
	 * set type and native class to typeMap.
	 * @param type
	 * @param allowUnresolvedClass
	 * - If true, do not set native class.
	 * @return
	 */
	private Type setTypeAndThrowIfDefined(Type type, boolean allowUnresolvedClass) {
		/**
		 * check type duplication.
		 */
		String typeName = type.getTypeName();
		if(this.typeMap.containsKey(typeName)) {
			throw new RuntimeException(typeName + " has already defined.");
		}
		/**
		 * add type to typeMap.
		 */
		this.typeMap.put(typeName, type);

		if(type.hasPairedClass()) {
			Class<?> nativeClass = type.getNativeClass();
			if(allowUnresolvedClass) {
				return type;
			}
			assert nativeClass != null;
			if(this.class2TypeMap.containsKey(nativeClass)) {
				throw new RuntimeException("native class " + nativeClass.getName() + " has already exported.");
			}
			this.class2TypeMap.put(nativeClass, type);
		}
		return type;
	}

	/**
	 * set generated native class.
	 * It is used for ClassType.
	 * @param type
	 * @param nativeClass
	 */
	public void setGeneratedNativeClass(UserDefinedType type, Class<?> nativeClass) {
		String typeName = type.getTypeName();
		if(!this.typeMap.containsKey(typeName)) {
			throw new RuntimeException("undefined type: " + typeName);
		}
		if(this.class2TypeMap.containsKey(nativeClass)) {
			throw new RuntimeException("native class " + nativeClass.getName() + " has already defined.");
		}
		this.class2TypeMap.put(nativeClass, type);
		type.setGeneratedClass(nativeClass);
	}

	public ClassType createAndSetClassType(String className, Type superType) {
		if(!superType.allowExtends) {
			throw new RuntimeException(superType.getTypeName() + " is not inheritable");
		}
		Type type = new ClassType(className, superType);
		return (ClassType) this.setTypeAndThrowIfDefined(type, true);
	}

	/**
	 * 
	 * @param typeName
	 * @return
	 * - if type is not defined, return unresolvedType.
	 * cannot get generic base type.
	 */
	public Type getType(String typeName) {
		Type type = this.typeMap.get(typeName);
		if(type instanceof GenericBaseType) {
			throw new RuntimeException("cannot directly use generic type:" + type.getTypeName());
		}
		return type == null ? this.unresolvedType : type;
	}

	/**
	 * get generic base type.
	 * @param typeName
	 * @return
	 * - if type is undefined, throw exception.
	 */
	public Type getGenericBaseType(String typeName) {
		Type type = this.typeMap.get(typeName);
		if(!(type instanceof GenericBaseType)) {
			throw new RuntimeException("undefined generic base type: " + typeName);
		}
		return type;
	}

	/**
	 * get type except generic base type.
	 * @param typeName
	 * @return
	 * - if type is not defined, throw exception.
	 */
	public Type getTypeAndThrowIfUndefined(String typeName) {
		Type type = this.getType(typeName);
		if(!type.isResolvedType()) {
			throw new RuntimeException("undefined type: " + typeName);
		}
		return type;
	}

	public ClassType getClassType(String typeName) {
		Type type = this.getTypeAndThrowIfUndefined(typeName);
		if(type instanceof ClassType) {
			return (ClassType) type;
		}
		throw new RuntimeException(typeName + " is not class type");
	}

	private Type createAndGetTypeIfUndefined(Type type) {
		Type gottenType = this.getType(type.getTypeName());
		if(!gottenType.isResolvedType()) {
			return this.setTypeAndThrowIfDefined(type, true);
		}
		return gottenType;
	}

	public Type createAndGetArrayTypeIfUndefined(Type elementType) {
		return this.createAndGetTypeIfUndefined(new GenericArrayType((GenericBaseType) this.baseArrayType, elementType));
	}

	public Type createAndGetMapTypeIfUndefined(Type elementType) {
		return this.createAndGetTypeIfUndefined(new GenericMapType((GenericBaseType) this.baseMapType, elementType));
	}

	/**
	 * Currently user defined generic class not supported.
	 * Future may be supported.
	 * @param baseTypeName
	 * @param types
	 * @return
	 */
	public Type createAndGetGenericTypeIfUndefined(String baseTypeName, Type[] types) {
		GenericBaseType baseType = (GenericBaseType) this.getTypeAndThrowIfUndefined(baseTypeName);
		return this.createAndGetTypeIfUndefined(new GenericType(baseType, types));
	}

	public Type createAndGetFuncTypeIfUndefined(Type returnType, Type[] paramsType) {
		return this.createAndGetTypeIfUndefined(new FunctionType(returnType, paramsType));
	}

	public static String toGenericTypeName(GenericBaseType baseType, Type type) {
		return toGenericTypeName(baseType, new Type[]{type});
	}

	public static String toGenericTypeName(GenericBaseType baseType, Type[] types) {
		StringBuilder sBuilder = new StringBuilder();
		if(baseType.getNativeClass().equals(GenericArray.class)) {
			sBuilder.append(types[0].toString());
			sBuilder.append("[]");
		}
		else {
			sBuilder.append(baseType.getTypeName());
			sBuilder.append("<");
			for(int i = 0; i < types.length; i++) {
				if(i > 0) {
					sBuilder.append(",");
				}
				sBuilder.append(types[i].getTypeName());
			}
			sBuilder.append(">");
		}
		return sBuilder.toString();
	}

	public static String toFuncTypeName(Type returnType, Type[] paramsType) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("Func<");
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

		/**
		 * If true, represents final type.
		 */
		private final boolean allowExtends;

		protected Type(String typeName, Class<?> nativeClass) {
			this(typeName, nativeClass, true);
		}

		protected Type(String typeName, Class<?> nativeClass, boolean allowExtends) {
			this.typeName = typeName;
			this.nativeClass = nativeClass;
			this.allowExtends = allowExtends;
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

		/**
		 * for type checker
		 * @return
		 */
		public boolean isResolvedType() {
			return true;
		}

		/**
		 * if true, can extends this type.
		 * @return
		 */
		public final boolean allowExtends() {
			return allowExtends;
		}

		public boolean hasPairedClass() {
			return true;
		}
	}

	/**
	 * Represents primitive type (int, float, boolean type).
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class PrimitiveType extends Type {
		private final Class<?> wrapperClass;
		private final Class<?> proxyClass;

		private PrimitiveType(String typeName, Class<?> nativeClass) {
			super(typeName, nativeClass, false);
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
			super("void", Void.class, false);
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
			super("", null, false);
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

		public GenericBaseType getBaseType() {
			return this.baseType;
		}

		public Type[] getElementsType() {
			return this.elementsType;
		}

		@Override
		public boolean hasPairedClass() {
			return false;
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
			super(typeName, nativeClass, false);
		}
	}

	/**
	 * Represents generic array type.
	 * It contains element type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class GenericArrayType extends GenericType {
		private GenericArrayType(GenericBaseType baseType, Type elementType) {
			super(baseType, new Type[]{elementType});
		}
	}

	public final static class PrimitiveArrayType extends GenericArrayType {
		private final Class<?> primitiveArrayClass;

		private PrimitiveArrayType(GenericBaseType baseType, PrimitiveType elementType) {
			super(baseType, elementType);
			if(elementType.getNativeClass().equals(long.class)) {
				this.primitiveArrayClass = IntArray.class;
			} else if(elementType.getNativeClass().equals(double.class)) {
				this.primitiveArrayClass = FloatArray.class;
			} else if(elementType.getNativeClass().equals(boolean.class)) {
				this.primitiveArrayClass = BooleanArray.class;
			} else {
				throw new RuntimeException(elementType.getNativeClass().getCanonicalName() + " is unsupported class");
			}
		}

		@Override
		public Class<?> getNativeClass() {
			return this.primitiveArrayClass;
		}

		@Override
		public boolean hasPairedClass() {
			return true;
		}
	}

	/**
	 * Represents generic map type.
	 * It contains element type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public final static class GenericMapType extends GenericType {
		private GenericMapType(GenericBaseType baseType, Type elementType) {
			super(baseType, new Type[]{elementType});
		}
	}

	public static class UserDefinedType extends Type {
		protected Class<?> generatedClass;

		protected UserDefinedType(boolean allowExtends) {
			super("", null, allowExtends);
		}

		public void setGeneratedClass(Class<?> generatedClass) {
			this.generatedClass = generatedClass;
		}

		@Override
		public Class<?> getNativeClass() {
			return this.generatedClass;
		}
	}
	/**
	 * Represents function type.
	 * It contains parameters type and return type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FunctionType extends UserDefinedType {
		private final String funcTypeName;
		private final Type returnType;
		private final Type[] paramsType;

		private FunctionType(Type returnType, Type[] paramsType) {
			super(false);
			this.returnType = returnType;
			this.paramsType = paramsType;
			this.funcTypeName = TypePool.toFuncTypeName(this.returnType, this.paramsType);
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
	public final static class ClassType extends UserDefinedType {
		private final Type superType;

		/**
		 * key is FuncType name.
		 */

		/**
		 * key is field name.
		 */
		private final HashMap<String, FieldHandle> fieldHandleMap;

		/**
		 * key id method name.
		 */
		private final HashMap<String, MethodHandle> methodHandlMap;

		private ClassType(String className, Type superType) {
			super(true);
			this.superType = superType;
			this.fieldHandleMap = new HashMap<>();
			this.methodHandlMap = new HashMap<>();
		}

		public Type getSuperType() {
			return this.superType;
		}
	}

	/**
	 * Used for ClassType.
	 * contains class field's (except for FuncType field) name and type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private static class FieldHandle {
		public final String name;
		public final Type type;

		private FieldHandle(String name, Type type) {
			this.name = name;
			this.type = type;
		}
	}

	/**
	 * Used for ClassType.
	 * contains FuncType field's name and type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private static class MethodHandle {
		public final String name;

		/**
		 *  Func<$returnType$, [$recvType, $argType...]>
		 */
		public final FunctionType type;

		private MethodHandle(String name, FunctionType type) {
			this.name = name;
			this.type = type;
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
			super("$unresolved$", null, false);
		}

		@Override
		public Class<?> getNativeClass() {
			throw new RuntimeException("cannot call this method");
		}

		@Override
		public boolean isResolvedType() {
			return false;
		}

		@Override
		public boolean hasPairedClass() {
			return false;
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
