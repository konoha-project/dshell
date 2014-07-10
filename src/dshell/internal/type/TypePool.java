package dshell.internal.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dshell.internal.codegen.JavaByteCodeGen;
import dshell.internal.lib.DShellClassLoader;
import dshell.internal.lib.Utils;
import dshell.internal.parser.error.TypeLookupException;
import dshell.internal.type.DSType.BoxedPrimitiveType;
import dshell.internal.type.DSType.FuncHolderType;
import dshell.internal.type.DSType.FunctionBaseType;
import dshell.internal.type.DSType.FunctionType;
import dshell.internal.type.DSType.PrimitiveType;
import dshell.internal.type.DSType.UnresolvedType;
import dshell.internal.type.DSType.VoidType;
import dshell.internal.type.DSType.RootClassType;
import dshell.internal.type.ParametricType.ParametricGenericType;

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
	public final GenericBaseType basePairType;

	public final FunctionBaseType baseFuncType;

	/**
	 * boxed primitive type. used for generic type.
	 */
	private final BoxedPrimitiveType boxedIntType;
	private final BoxedPrimitiveType boxedFloaType;
	private final BoxedPrimitiveType boxedBooleanType;
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
		this.intType         = (PrimitiveType) this.setTypeAndThrowIfDefined(new PrimitiveType("int", "long"));
		this.floatType       = (PrimitiveType) this.setTypeAndThrowIfDefined(new PrimitiveType("float", "double"));
		this.booleanType     = (PrimitiveType) this.setTypeAndThrowIfDefined(new PrimitiveType("boolean", "boolean"));
		this.stringType          = (ClassType) this.setTypeAndThrowIfDefined(TypeInitializer.init_StringWrapper(this));
		this.exceptionType       = (ClassType) this.setTypeAndThrowIfDefined(TypeInitializer.init_Exception(this));
		this.baseArrayType = (GenericBaseType) this.setTypeAndThrowIfDefined(TypeInitializer.init_GenericArray(this));
		this.baseMapType   = (GenericBaseType) this.setTypeAndThrowIfDefined(TypeInitializer.init_GenericMap(this));
		this.basePairType  = (GenericBaseType) this.setTypeAndThrowIfDefined(TypeInitializer.init_GenericPair(this));
		this.baseFuncType = (FunctionBaseType) this.setTypeAndThrowIfDefined(new FunctionBaseType());

		this.setTypeAndThrowIfDefined(TypeInitializer.init_IntArray(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_FloatArray(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_BooleanArray(this));

		this.setTypeAndThrowIfDefined(TypeInitializer.init_KeyNotFoundException(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_OutOfIndexException(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_ArithmeticException(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_TypeCastException(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_DShellException(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_NullException(this));

		this.setTypeAndThrowIfDefined(TypeInitializer.init_InputStream(this));
		this.setTypeAndThrowIfDefined(TypeInitializer.init_OutputStream(this));

		this.boxedIntType     = new BoxedPrimitiveType(this.intType);
		this.boxedFloaType    = new BoxedPrimitiveType(this.floatType);
		this.boxedBooleanType = new BoxedPrimitiveType(this.booleanType);
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
			throw new TypeLookupException("cannot directly use generic type: " + type.getTypeName());
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
	public GenericBaseType getGenericBaseType(String typeName, int elementSize) {
		DSType type = this.typeMap.get(typeName);
		if(type instanceof GenericBaseType) {
			GenericBaseType baseType = (GenericBaseType) type;
			if(baseType.getElementSize() != elementSize) {
				throw new TypeLookupException(
						typeName + " requires " + baseType.getElementSize() + " element, but element size is " + elementSize);
			}
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
	 * get boxed primitive type.
	 * @param type
	 * @return
	 */
	public BoxedPrimitiveType getBoxedPrimitiveType(PrimitiveType type) {
		if(type.equals(this.intType)) {
			return this.boxedIntType;
		}
		if(type.equals(this.floatType)) {
			return this.boxedFloaType;
		}
		if(type.equals(this.booleanType)) {
			return this.boxedBooleanType;
		}
		Utils.fatal(1, "unsuppoirted primitive type: " + type);
		return null;
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
	 * @param typeList
	 * @return
	 */
	public DSType createAndGetReifiedTypeIfUndefined(String baseTypeName, List<DSType> typeList) {
		GenericBaseType baseType = this.getGenericBaseType(baseTypeName, typeList.size());
		String typeName = toGenericTypeName(baseType, typeList);
		DSType genericType = this.getType(typeName);
		if(genericType instanceof UnresolvedType) {
			genericType = new ReifiedType(this, typeName, baseType, typeList);
			this.typeMap.put(typeName, genericType);
		}
		return genericType;
	}

	public FunctionType createAndGetFuncTypeIfUndefined(DSType returnType, List<DSType> paramTypeList) {
		String typeName = toFuncTypeName(returnType, paramTypeList);
		DSType funcType = this.getType(typeName);
		if(funcType instanceof UnresolvedType) {
			String internalName = generatedFuncNamePrefix + "FuncType" + ++funcNameSuffix;
			funcType = new FunctionType(typeName, internalName, returnType, paramTypeList);
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
	 * @param typeList
	 * @return
	 */
	public static String toGenericTypeName(GenericBaseType baseType, List<DSType> typeList) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append(baseType.getTypeName());
		sBuilder.append("<");
		int size = typeList.size();
		for(int i = 0; i < size; i++) {
			if(i > 0) {
				sBuilder.append(",");
			}
			sBuilder.append(typeList.get(i).getTypeName());
		}
		sBuilder.append(">");
		return sBuilder.toString();
	}

	public static String toFuncTypeName(DSType returnType, List<DSType> paramTypeList) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("Func<");
		sBuilder.append(returnType.toString());

		int size = paramTypeList.size();
		if(size > 0) {
			sBuilder.append(",[");
			for(int i = 0; i < size; i++) {
				if(i > 0) {
					sBuilder.append(",");
				}
				sBuilder.append(paramTypeList.get(i).toString());
			}
			sBuilder.append("]");
		}
		sBuilder.append(">");
		return sBuilder.toString();
	}

	public DSType parseTypeName(String typeName) {	//FIXME: only support generic type,
		if(typeName.indexOf('<') == -1) {
			if(typeName.startsWith("@")) {
				return new ParametricType(typeName);
			}
			return this.getTypeAndThrowIfUndefined(typeName);
		}
		return new ParseContext(this, typeName).parse();
	}

	/**
	 * type parser for generic type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private class ParseContext {
		private final TypePool pool;
		private final String source;
		private final int size;
		private int index;

		private ParseContext(TypePool pool, String source) {
			this.pool = pool;
			this.source = source;
			this.size = source.length();
			this.index = 0;
		}

		private DSType parse() {
			final int startIndex = this.index;
			for(; this.index < this.size; this.index++) {
				char ch = this.source.charAt(this.index);
				switch(ch) {
				case '<':
					String baseTypeName = this.source.substring(startIndex, this.index++);
					List<DSType> typeList = new ArrayList<>();
					this.consumeSpace();
					typeList.add(this.parse());
					while(this.source.charAt(this.index) != '>') {
						this.consumeSeparator();
						typeList.add(this.parse());
					}
					return this.createGenericType(baseTypeName, typeList);
				case '>':
				case ',': {
					String typeName = this.source.substring(startIndex, this.index);
					if(typeName.startsWith("@")) {
						return new ParametricType(typeName);
					}
					return this.pool.getTypeAndThrowIfUndefined(typeName);
				}
				case ' ': {
					String typeName = this.source.substring(startIndex, this.index);
					this.consumeSpace();
					if(typeName.startsWith("@")) {
						return new ParametricType(typeName);
					}
					return this.pool.getTypeAndThrowIfUndefined(typeName);
				}
				default:
					break;
				}
			}
			throw new RuntimeException("illeagal type name: " + this.source);
		}

		private void consumeSpace() {
			for(; this.index < this.size; this.index++) {
				if(this.source.charAt(this.index) != ' ') {
					break;
				}
			}
		}

		private void consumeSeparator() {
			int separatorCount = 0;
			for(; this.index < this.size; this.index++) {
				switch(this.source.charAt(this.index)) {
				case ' ':
					break;
				case ',':
					separatorCount++;
					break;
				default:
					if(separatorCount != 0) {
						throw new RuntimeException("illegal separator count: " + separatorCount + ", " + this.source);
					}
					return;
				}
			}
			throw new RuntimeException("found problem: " + this.source);
		}

		private DSType createGenericType(String baseTypeName, List<DSType> elementTypeList) {
			boolean foundParametricType = false;
			for(DSType elementType : elementTypeList) {
				if((elementType instanceof ParametricType) || (elementType instanceof ParametricGenericType)) {
					foundParametricType = true;
					break;
				}
			}
			if(foundParametricType) {
				return new ParametricGenericType(baseTypeName, elementTypeList);
			} else {
				return this.pool.createAndGetReifiedTypeIfUndefined(baseTypeName, elementTypeList);
			}
		}
	}

	public static void main(String[] args) {
		TypePool pool = new TypePool(new DShellClassLoader());
		String source = "Array<Array<@T>>";
		DSType parsedType = pool.parseTypeName(source);
		System.out.println(parsedType);
	}
}
