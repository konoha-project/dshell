package dshell.internal.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dshell.internal.codegen.JavaByteCodeGen;
import dshell.internal.lib.DShellClassLoader;
import dshell.internal.parser.error.TypeLookupException;
import dshell.internal.type.DSType.FuncHolderType;
import dshell.internal.type.DSType.FunctionBaseType;
import dshell.internal.type.DSType.FunctionType;
import dshell.internal.type.DSType.PrimitiveType;
import dshell.internal.type.DSType.UnresolvedType;
import dshell.internal.type.DSType.VoidType;
import dshell.internal.type.DSType.RootClassType;
import dshell.internal.type.ReifiedType.GenericBaseType;

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
	public final static DSType unresolvedType = new UnresolvedType();

	/**
	 * Equivalent to java void.
	 */
	public final static VoidType voidType = new VoidType();

	/**
	 * Equivalent to java long.
	 */
	public final PrimitiveType intType;

	/**
	 * Equivalent to java double.
	 */
	public final PrimitiveType floatType;

	/**
	 * Equivalent to java boolean.
	 */
	public final PrimitiveType booleanType;

	/**
	 * Represents D-Shell root class type.
	 * It is equivalent to java Object.
	 */
	public final DSType objectType = new RootClassType();

	/**
	 * Represents D-Shell string type.
	 */
	public final ClassType stringType;

	/**
	 * Represents D-Shell root exception type.
	 */
	public final ClassType exceptionType;

	public final GenericBaseType baseArrayType;
	public final GenericBaseType baseMapType;

	public final FunctionBaseType baseFuncType;

	/**
	 * type name to type translation table
	 */
	protected final HashMap<String, DSType> typeMap;

	/**
	 * class loader for FuncType generation.
	 * do not use it for other purpose.
	 */
	protected final DShellClassLoader classLoader;

	public TypePool(DShellClassLoader classLoader) {
		this.classLoader = classLoader;
		this.typeMap = new HashMap<>();

		this.setTypeAndThrowIfDefined(voidType);
		this.intType       = (PrimitiveType) this.setTypeAndThrowIfDefined(new PrimitiveType("int", "long"));
		this.floatType     = (PrimitiveType) this.setTypeAndThrowIfDefined(new PrimitiveType("float", "double"));
		this.booleanType   = (PrimitiveType) this.setTypeAndThrowIfDefined(new PrimitiveType("boolean", "boolean"));
		this.stringType = (ClassType) this.setTypeAndThrowIfDefined(TypeInitializer.init_StringWrapper(this));
		this.exceptionType = (ClassType) this.setTypeAndThrowIfDefined(TypeInitializer.init_Exception(this));
		this.baseArrayType = (GenericBaseType) this.setTypeAndThrowIfDefined(new GenericBaseType("Array", "dshell/lang/GenericArray", this.objectType, false));
		this.baseMapType   = (GenericBaseType) this.setTypeAndThrowIfDefined(new GenericBaseType("Map", "dshell/lang/GenericMap", this.objectType, false));
		this.baseFuncType = (FunctionBaseType) this.setTypeAndThrowIfDefined(new FunctionBaseType());
		
		this.setTypeAndThrowIfDefined(TypeInitializer.init_IntArray(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_FloatArray(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_BooleanArray(this));

		this.setTypeAndThrowIfDefined(TypeInitializer.init_KeyNotFoundException(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_OutOfIndexException(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_ArithmeticException(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_DShellException(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_NullException(this));
	}

	private DSType setTypeAndThrowIfDefined(DSType type) {
		if(this.typeMap.containsKey(type.getTypeName())) {
			throw new TypeLookupException(type.getTypeName() + " is already defined");
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
	public DSType getType(String typeName) {
		DSType type = this.typeMap.get(typeName);
		if(type instanceof GenericBaseType) {
			throw new TypeLookupException("cannot directly use generic type:" + type.getTypeName());
		}
		return type == null ? TypePool.unresolvedType : type;
	}

	/**
	 * get type except generic base type.
	 * @param typeName
	 * @return
	 * - if type is undefined, throw exception.
	 */
	public DSType getTypeAndThrowIfUndefined(String typeName) {
		DSType type = this.getType(typeName);
		if(type instanceof UnresolvedType) {
			throw new TypeLookupException("undefined type: " + typeName);
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
		DSType type = this.typeMap.get(typeName);
		if(type instanceof GenericBaseType) {
			return (GenericBaseType) type;
		}
		throw new TypeLookupException(typeName + " is not generic base type");
	}

	/**
	 * get primitive type
	 * @param typeName
	 * @return
	 * - if undefined, throw exception.
	 */
	public PrimitiveType getPrimitiveType(String typeName) {
		DSType type = this.getTypeAndThrowIfUndefined(typeName);
		if(type instanceof PrimitiveType) {
			return (PrimitiveType) type;
		}
		throw new TypeLookupException(typeName + " is not primitive type");
	}

	/**
	 * get class type.
	 * @param typeName
	 * @return
	 * - if undefined, throw exception.
	 */
	public ClassType getClassType(String typeName) {
		DSType type = this.getTypeAndThrowIfUndefined(typeName);
		if(type instanceof ClassType) {
			return (ClassType) type;
		}
		throw new TypeLookupException(typeName + " is not class type");
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
	public ClassType createAndSetClassType(String className, DSType superType) {
		if(!superType.allowExtends()) {
			throw new TypeLookupException(superType.getTypeName() + " is not inheritable");
		}
		if(!(this.getType(className) instanceof UnresolvedType)) {
			throw new TypeLookupException(className + " is already defined.");
		}
		ClassType classType = new UserDefinedClassType(className, generatedClassNamePrefix + className, superType, true);
		this.typeMap.put(className, classType);
		return classType;
	}

	/**
	 * Currently user defined generic class not supported.
	 * Future may be supported.
	 * @param baseTypeName
	 * @param types
	 * @return
	 */
	public DSType createAndGetReifiedTypeIfUndefined(String baseTypeName, DSType[] types) {
		GenericBaseType baseType = this.getGenericBaseType(baseTypeName);
		String typeName = toGenericTypeName(baseType, types);
		DSType genericType = this.getType(typeName);
		if(genericType instanceof UnresolvedType) {
			List<DSType> typeList = new ArrayList<>(types.length);
			for(DSType elementType : types) {
				typeList.add(elementType);
			}
			genericType = new ReifiedType(typeName, baseType, typeList);
			this.typeMap.put(typeName, genericType);
		}
		return genericType;
	}

	public FunctionType createAndGetFuncTypeIfUndefined(DSType returnType, DSType[] paramTypes) {
		String typeName = toFuncTypeName(returnType, paramTypes);
		DSType funcType = this.getType(typeName);
		if(funcType instanceof UnresolvedType) {
			List<DSType> typeList = new ArrayList<>(paramTypes.length);
			for(DSType paramType : paramTypes) {
				typeList.add(paramType);
			}
			String internalName = generatedFuncNamePrefix + "FuncType" + ++funcNameSuffix;
			funcType = new FunctionType(typeName, internalName, returnType, typeList);
			this.typeMap.put(typeName, funcType);
			this.classLoader.definedAndLoadClass(internalName, JavaByteCodeGen.generateFuncTypeInterface((FunctionType) funcType));
		}
		return (FunctionType) funcType;
	}

	public FuncHolderType createFuncHolderType(FunctionType funcType, String funcName) {
		String typeName = "FuncHolder" + ++funcNameSuffix + "of" + funcType.getTypeName();
		String internalName = generatedFuncNamePrefix + "FuncHolder" + funcNameSuffix + "_" + funcName;
		return new FuncHolderType(typeName, internalName, funcType);
	}

	// type name creator api.
	/**
	 * crate generic type name except for generic array.
	 * @param baseType
	 * @param types
	 * @return
	 */
	public static String toGenericTypeName(GenericBaseType baseType, DSType[] types) {
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

	public static String toFuncTypeName(DSType returnType, DSType[] paramTypes) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("Func<");
		sBuilder.append(returnType.toString());
		
		if(paramTypes.length > 0) {
			sBuilder.append(",[");
			for(int i = 0; i < paramTypes.length; i++) {
				if(i > 0) {
					sBuilder.append(",");
				}
				sBuilder.append(paramTypes[i].toString());
			}
			sBuilder.append("]");
		}
		sBuilder.append(">");
		return sBuilder.toString();
	}

	public DSType parseTypeName(String typeName) {
		if(typeName.indexOf('<') == -1) {
			return this.getType(typeName);
		}
		throw new RuntimeException("illegal type name: " + typeName);
	}
}
