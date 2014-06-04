package dshell.internal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dshell.internal.codegen.JavaByteCodeGen;
import dshell.internal.lib.DShellClassLoader;
import dshell.internal.parser.CalleeHandle.ConstructorHandle;
import dshell.internal.parser.CalleeHandle.FieldHandle;
import dshell.internal.parser.CalleeHandle.FunctionHandle;
import dshell.internal.parser.CalleeHandle.MethodHandle;
import dshell.internal.parser.CalleeHandle.StaticFieldHandle;
import dshell.internal.parser.CalleeHandle.StaticFunctionHandle;

/**
 * It contains builtin types ant user defined types.
 * @author skgchxngsxyz-osx
 *
 */
public class TypePool {
	/**
	 * package name for generated class.
	 */
	private final static String generatedClassNamePrefix = "dshell/defined/class/";

	/**
	 * package name for generated func interface.
	 */
	private final static String generatedFuncNamePrefix = "dshell/defined/func/";

	private static int funcNameSuffix = -1;

	/**
	 * used for type checker.
	 * it is default value of ExprNode type.
	 */
	public final static Type unresolvedType = new UnresolvedType();

	/**
	 * Equivalent to java void.
	 */
	public final Type voidType;

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
	 * Represents D-Shell root class type.
	 * It is equivalent to java Object.
	 */
	public final Type objectType;

	/**
	 * Represents D-Shell string type.
	 */
	public final ClassType stringType;

	/**
	 * Represents D-Shell root exception type.
	 */
	public final ClassType exceptionType;

	private final GenericBaseType baseArrayType;
	private final GenericBaseType baseMapType;

	private final FunctionBaseType baseFuncType;

	/**
	 * type name to type translation table
	 */
	protected final HashMap<String, Type> typeMap;

	/**
	 * class loader for FuncType generation.
	 * do not use it for other purpose.
	 */
	protected final DShellClassLoader classLoader;

	public TypePool(DShellClassLoader classLoader) {
		this.classLoader = classLoader;
		this.typeMap = new HashMap<>();
		/**
		 * do not set to TypeMap;
		 */
		this.objectType = new RootClassType();
		this.baseArrayType = new GenericBaseType("Array", "dshell/lang/GenericArray", objectType, false);

		this.voidType      = this.setTypeAndThrowIfDefined(new VoidType());
		this.intType       = this.setTypeAndThrowIfDefined(new PrimitiveType("int", "long"));
		this.floatType     = this.setTypeAndThrowIfDefined(new PrimitiveType("float", "double"));
		this.booleanType   = this.setTypeAndThrowIfDefined(new PrimitiveType("boolean", "boolean"));
		this.stringType    = (ClassType) this.setTypeAndThrowIfDefined(new ClassType("String", "java/lang/String", this.objectType, false));
		this.exceptionType = (ClassType) this.setTypeAndThrowIfDefined(new ClassType("Exception", "dshell/lang/Exception", this.objectType, true));
		this.baseMapType   = (GenericBaseType) this.setTypeAndThrowIfDefined(new GenericBaseType("Map", "dshell/lang/GenericMap", this.objectType, false));
		this.baseFuncType = (FunctionBaseType) this.setTypeAndThrowIfDefined(new FunctionBaseType());
//
//		/**
//		 * add primitive array type.
//		 */
//		this.setTypeAndThrowIfDefined(new PrimitiveArrayType((GenericBaseType) this.baseArrayType, (PrimitiveType) this.intType));
//		this.setTypeAndThrowIfDefined(new PrimitiveArrayType((GenericBaseType) this.baseArrayType, (PrimitiveType) this.floatType));
//		this.setTypeAndThrowIfDefined(new PrimitiveArrayType((GenericBaseType) this.baseArrayType, (PrimitiveType) this.booleanType));
	}

	private Type setTypeAndThrowIfDefined(Type type) {
		if(this.typeMap.containsKey(type.getTypeName())) {
			throw new RuntimeException(type.getTypeName() + " is already defined");
		}
		this.typeMap.put(type.getTypeName(), type);
		return type;
	}

	// type getter api
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
		return type == null ? TypePool.unresolvedType : type;
	}

	/**
	 * get type except generic base type.
	 * @param typeName
	 * @return
	 * - if type is undefined, throw exception.
	 */
	public Type getTypeAndThrowIfUndefined(String typeName) {
		Type type = this.getType(typeName);
		if(type instanceof UnresolvedType) {
			throw new RuntimeException("undefined type: " + typeName);
		}
		return type;
	}

	/**
	 * get generic base type.
	 * @param typeName
	 * @return
	 * - if type is undefined, throw exception.
	 */
	public GenericBaseType getGenericBaseType(String typeName) {
		Type type = this.getTypeAndThrowIfUndefined(typeName);
		if(type instanceof GenericBaseType) {
			return (GenericBaseType) type;
		}
		throw new RuntimeException(typeName + " is not generic base type");
	}

	/**
	 * get primitive type
	 * @param typeName
	 * @return
	 * - if undefined, throw exception.
	 */
	public PrimitiveType getPrimitiveType(String typeName) {
		Type type = this.getTypeAndThrowIfUndefined(typeName);
		if(type instanceof PrimitiveType) {
			return (PrimitiveType) type;
		}
		throw new RuntimeException(typeName + " is not primitive type");
	}

	/**
	 * get class type.
	 * @param typeName
	 * @return
	 * - if undefined, throw exception.
	 */
	public ClassType getClassType(String typeName) {
		Type type = this.getTypeAndThrowIfUndefined(typeName);
		if(type instanceof ClassType) {
			return (ClassType) type;
		}
		throw new RuntimeException(typeName + " is not class type");
	}

	// type creator api
	/**
	 * create class and set to typemap.
	 * @param className
	 * - user defined class name.
	 * @param superType
	 * @return
	 * - generated class type
	 */
	public ClassType createAndSetClassType(String className, Type superType) {
		if(!superType.allowExtends()) {
			throw new RuntimeException(superType.getTypeName() + " is not inheritable");
		}
		if(!(this.getType(className) instanceof UnresolvedType)) {
			throw new RuntimeException(className + " is already defined.");
		}
		ClassType classType = new ClassType(className, generatedClassNamePrefix + className, superType, true);
		this.typeMap.put(className, classType);
		return classType;
	}

	public GenericArrayType createAndGetArrayTypeIfUndefined(Type elementType) {
		String typeName = toGenericArrayTypeName(elementType);
		Type arrayType = this.getType(typeName);
		if(arrayType instanceof UnresolvedType) {
			arrayType = new GenericArrayType(this.baseArrayType, elementType);
			assert typeName.equals(arrayType.getTypeName());
			this.typeMap.put(typeName, arrayType);
		}
		return (GenericArrayType) arrayType;
	}

	public GenericType createAndGetMapTypeIfUndefined(Type elementType) {
		return this.createAndGetGenericTypeIfUndefined("Map", new Type[]{elementType});
	}

	/**
	 * Currently user defined generic class not supported.
	 * Future may be supported.
	 * @param baseTypeName
	 * @param types
	 * @return
	 */
	public GenericType createAndGetGenericTypeIfUndefined(String baseTypeName, Type[] types) {
		GenericBaseType baseType = this.getGenericBaseType(baseTypeName);
		String typeName = toGenericTypeName(baseType, types);
		Type genericType = this.getType(typeName);
		if(genericType instanceof UnresolvedType) {
			List<Type> typeList = new ArrayList<>(types.length);
			for(Type elementType : types) {
				typeList.add(elementType);
			}
			genericType = new GenericType(typeName, baseType, typeList);
			this.typeMap.put(typeName, genericType);
		}
		return (GenericType) genericType;
	}

	public FunctionType createAndGetFuncTypeIfUndefined(Type returnType, Type[] paramTypes) {
		String typeName = toFuncTypeName(returnType, paramTypes);
		Type funcType = this.getType(typeName);
		if(funcType instanceof UnresolvedType) {
			List<Type> typeList = new ArrayList<>(paramTypes.length);
			for(Type paramType : paramTypes) {
				typeList.add(paramType);
			}
			String internalName = generatedFuncNamePrefix + "FuncType" + ++funcNameSuffix;
			funcType = new FunctionType(typeName, internalName, returnType, typeList);
			this.typeMap.put(typeName, funcType);
			this.classLoader.definedAndLoadClass(internalName, JavaByteCodeGen.generateFuncTypeInterface((FunctionType) funcType));
		}
		return (FunctionType) funcType;
	}

	public FuncHolderType createFuncHolderType(FunctionType funcType) {
		String typeName = "FuncHolder" + ++funcNameSuffix + "of" + funcType.getTypeName();
		String internalName = generatedFuncNamePrefix + "FuncHolder" + funcNameSuffix;
		return new FuncHolderType(typeName, internalName, funcType);
	}

	// type name creator api.
	/**
	 * crate generic type name except for generic array.
	 * @param baseType
	 * @param types
	 * @return
	 */
	public static String toGenericTypeName(GenericBaseType baseType, Type[] types) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append(baseType.getTypeName());
		sBuilder.append("<");
		for(int i = 0; i < types.length; i++) {
			if(i > 0) {
				sBuilder.append(",");
			}
			sBuilder.append(types[i].getTypeName());
		}
		sBuilder.append(">");
		return sBuilder.toString();
	}

	public static String toGenericArrayTypeName(Type elementType) {
		return elementType.getTypeName() + "[]";
	}

	public static String toFuncTypeName(Type returnType, Type[] paramTypes) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("Func<");
		sBuilder.append(returnType.toString());
		for(Type paramType : paramTypes) {
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
	public static abstract class Type {
		/**
		 * String representation of this type.
		 * Must be unique name.
		 */
		private final String typeName;

		/**
		 * Represent fully qualify class name.
		 */
		private final String internalName;

		/**
		 * If true, represents final type.
		 */
		private final boolean allowExtends;

		protected Type(String typeName, String internalName) {
			this(typeName, internalName, true);
		}

		protected Type(String typeName, String internalName, boolean allowExtends) {
			this.typeName = typeName;
			this.internalName = internalName;
			this.allowExtends = allowExtends;
		}

		public String getTypeName() {
			return this.typeName;
		}

		public String getInternalName() {
			return this.internalName;
		}

		@Override
		public String toString() {
			return this.typeName;
		}

		/**
		 * if true, can extends this type.
		 * @return
		 */
		public final boolean allowExtends() {
			return allowExtends;
		}

		/**
		 * check inheritance of targetType.
		 * @param targetType
		 * @return
		 * - if this type is equivalent to targte type or is the super type of target type, return true;
		 */
		public boolean isAssignableFrom(Type targetType) {
			return this.getTypeName().equals(targetType.getTypeName());
		}

		/**
		 * loop up constructor handle.
		 * @param paramTypeList
		 * @return
		 * - return null, has no matched constructor.
		 */
		public ConstructorHandle lookupConstructorHandle(List<Type> paramTypeList) {
			return null;
		}

		/**
		 * loop up field handle.
		 * @param fieldName
		 * @return
		 * - return null, has no matched field.
		 */
		public FieldHandle lookupFieldHandle(String fieldName) {
			return null;
		}

		/**
		 * look up method handle.
		 * @param methodName
		 * @param paramTypeList
		 * @return
		 * - return null. has no matched method.
		 */
		public MethodHandle lookupMethodHandle(String methodName) {
			return null;
		}
		/**
		 * if called, cannot change this class element.
		 */
		public void finalizeType() {
		}
	}

	/**
	 * Represents primitive type (int, float, boolean type).
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class PrimitiveType extends Type {
		private PrimitiveType(String typeName, String internalName) {
			super(typeName, internalName, false);
		}
	}

	/**
	 * Represent void type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class VoidType extends Type {
		public VoidType() {
			super("void", "void", false);
		}
	}

	/**
	 * Represent class root type.
	 * it is equivalent for java.lang.object
	 * @author skgchxngsxyz-osx
	 *
	 */
	public final static class RootClassType extends Type {
		private RootClassType() {
			super("$Super$", "java/lang/Object");
		}

		@Override
		public boolean isAssignableFrom(Type targetType) {
			return targetType instanceof ClassType || targetType instanceof RootClassType;
		}
	}
	
	/**
	 * It is an initial value of expression node.
	 * Type checker replaces this type to resolved type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public final static class UnresolvedType extends Type {
		private UnresolvedType(){
			super("$unresolved$", "$unresolved$", false);
		}
	}

	/**
	 * Represent dshell function type interface.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static final class FunctionBaseType extends Type {
		protected FunctionBaseType() {
			super("Func", "dshell/lang/Function");
		}
	}

	/**
	 * Represents function type.
	 * It contains FunctionHandle.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FunctionType extends Type {
		private final FunctionHandle handle;

		protected FunctionType(String funcTypeName, String internalName, Type returnType, List<Type> paramTypeList) {
			super(funcTypeName, internalName, true);
			this.handle = new FunctionHandle(this, returnType, Collections.unmodifiableList(paramTypeList));
		}

		public FunctionHandle getHandle() {
			return this.handle;
		}
	}

	/**
	 * represent function holder type.
	 * used for function call and func field getter.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FuncHolderType extends Type {
		protected final StaticFieldHandle fieldHandle;
		protected final StaticFunctionHandle funcHandle;

		protected FuncHolderType(String typeName, String internalName, FunctionType funcType) {
			super(typeName, internalName, false);
			this.fieldHandle = new StaticFieldHandle("funcField", this, funcType);
			FunctionHandle handle = funcType.getHandle();
			this.funcHandle = new StaticFunctionHandle("invokeDirect", this, handle.getReturnType(), handle.getParamTypeList());
		}

		public StaticFieldHandle getFieldHandle() {
			return this.fieldHandle;
		}

		public StaticFunctionHandle getFuncHanle() {
			return this.funcHandle;
		}
	}

	/**
	 * Represents class type.
	 * Class name must be upper case.
	 * It contains super class type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ClassType extends Type {
		protected final Type superType;

		protected List<ConstructorHandle> constructorHandleList;

		/**
		 * key is field name.
		 */
		protected Map<String, FieldHandle> fieldHandleMap;

		/**
		 * key is method name.
		 */
		protected Map<String, MethodHandle> methodHandleMap;

		/**
		 * true after called finalizeType.
		 */
		protected boolean finalized = false;

		protected ClassType(String className, String internalName, Type superType, boolean allowExtends) {
			super(className, internalName, allowExtends);
			this.superType = superType;
			this.constructorHandleList = new ArrayList<>();
			this.fieldHandleMap = new HashMap<>();
			this.methodHandleMap = new HashMap<>();
		}

		public Type getSuperType() {
			return this.superType;
		}

		@Override
		public boolean isAssignableFrom(Type targetType) {
			if(!(targetType instanceof ClassType)) {
				return false;
			}
			if(this.getTypeName().equals(targetType.getTypeName())) {
				return true;
			}
			Type superType = ((ClassType)targetType).getSuperType();
			if(!(superType instanceof RootClassType)) {
				return this.isAssignableFrom(superType);
			}
			return false;
		}

		@Override
		public ConstructorHandle lookupConstructorHandle(List<Type> paramTypeList) {
			for(ConstructorHandle handle : this.constructorHandleList) {
				final int size = handle.getParamTypeList().size();
				if(size != paramTypeList.size()) {
					continue;
				}
				int matchCount = 0;
				for(int i = 0; i < size; i++) {
					if(!handle.getParamTypeList().get(i).isAssignableFrom(paramTypeList.get(i))) {
						break;
					}
					matchCount++;
				}
				if(matchCount == size) {
					return handle;
				}
			}
			return null;
		}

		@Override
		public FieldHandle lookupFieldHandle(String fieldName) {
			FieldHandle handle = this.fieldHandleMap.get(fieldName);
			if(handle == null) {
				return this.superType.lookupFieldHandle(fieldName);
			}
			return handle;
		}

		@Override
		public MethodHandle lookupMethodHandle(String methodName) {
			MethodHandle handle = this.methodHandleMap.get(methodName);
			if(handle == null) {
				return this.superType.lookupMethodHandle(methodName);
			}
			return handle;
		}

		@Override
		public void finalizeType() {
			if(!this.finalized) {
				this.finalized = true;
				this.constructorHandleList = Collections.unmodifiableList(this.constructorHandleList);
				this.fieldHandleMap = Collections.unmodifiableMap(this.fieldHandleMap);
				this.methodHandleMap = Collections.unmodifiableMap(this.methodHandleMap);
			}
		}
	}

	/**
	 * Represent base type of generic type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class GenericBaseType extends ClassType {
		protected GenericBaseType(String className, String internalName, Type superType, boolean allowExtends) {
			super(className, internalName, superType, allowExtends);
		}
	}

	/**
	 * Represents generic type (array type or map type).
	 * It contains elements type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class GenericType extends Type {
		private final GenericBaseType baseType;
		private final List<Type> elementTypeList;

		protected GenericType(String typeName, GenericBaseType baseType, List<Type> elementTypeList) {
			super(typeName, baseType.getInternalName(), false);
			this.baseType = baseType;
			this.elementTypeList = Collections.unmodifiableList(elementTypeList);
		}

		public GenericBaseType getBaseType() {
			return this.baseType;
		}

		public List<Type> getElementTypeList() {
			return this.elementTypeList;
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
			super(TypePool.toGenericArrayTypeName(elementType), baseType, toTypeList(elementType));
		}

		private static List<Type> toTypeList(Type type) {
			List<Type> typeList = new ArrayList<>(1);
			typeList.add(type);
			return typeList;
		}
	}

	public final static class PrimitiveArrayType extends GenericArrayType {
		private PrimitiveArrayType(GenericBaseType baseType, PrimitiveType elementType) {
			super(baseType, elementType);
		}
	}
}
