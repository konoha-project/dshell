package dshell.internal.type;

import java.util.Collections;
import java.util.List;

import dshell.internal.parser.CalleeHandle.ConstructorHandle;
import dshell.internal.parser.CalleeHandle.FieldHandle;
import dshell.internal.parser.CalleeHandle.FunctionHandle;
import dshell.internal.parser.CalleeHandle.MethodHandle;
import dshell.internal.parser.CalleeHandle.StaticFieldHandle;
import dshell.internal.parser.CalleeHandle.StaticFunctionHandle;

/**
 * Represents dshell type.
 * @author skgchxngsxyz-osx
 *
 */
public abstract class DSType {
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

	protected DSType(String typeName, String internalName) {
		this(typeName, internalName, true);
	}

	protected DSType(String typeName, String internalName, boolean allowExtends) {
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
	 * - if this type is equivalent to target type or is the super type of target type, return true;
	 */
	public boolean isAssignableFrom(DSType targetType) {
		if(!this.getClass().equals(targetType.getClass())) {
			return false;
		}
		return this.getTypeName().equals(targetType.getTypeName());
	}

	public boolean equals(DSType targetType) {
		return this.getTypeName().equals(targetType.getTypeName());
	}

	/**
	 * loop up constructor handle.
	 * @param paramTypeList
	 * @return
	 * - return null, has no matched constructor.
	 */
	public ConstructorHandle lookupConstructorHandle(List<DSType> paramTypeList) {
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

	/**
	 * Represents primitive type (int, float, boolean type).
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class PrimitiveType extends DSType {
		PrimitiveType(String typeName, String internalName) {
			super(typeName, internalName, false);
		}
	}

	/**
	 * Represent void type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class VoidType extends DSType {
		VoidType() {
			super("void", "void", false);
		}
	}

	/**
	 * Represent class root type.
	 * it is equivalent for java.lang.object
	 * @author skgchxngsxyz-osx
	 *
	 */
	public final static class RootClassType extends DSType {
		RootClassType() {
			super("$Super$", "java/lang/Object");
		}

		@Override
		public boolean isAssignableFrom(DSType targetType) {
			return targetType instanceof ClassType || targetType instanceof RootClassType;
		}
	}

	/**
	 * It is an initial value of expression node.
	 * Type checker replaces this type to resolved type.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public final static class UnresolvedType extends DSType {
		UnresolvedType(){
			super("$unresolved$", "$unresolved$", false);
		}
	}

	/**
	 * Represent dshell function type interface.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static final class FunctionBaseType extends DSType {
		FunctionBaseType() {
			super("Func", "dshell/lang/Function");
		}

		@Override
		public boolean isAssignableFrom(DSType targetType) {
			return targetType instanceof FunctionType || targetType instanceof FunctionBaseType;
		}
	}

	/**
	 * Represents function type.
	 * It contains FunctionHandle.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FunctionType extends DSType {
		private final FunctionHandle handle;

		FunctionType(String funcTypeName, String internalName, DSType returnType, List<DSType> paramTypeList) {
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
	public static class FuncHolderType extends DSType {
		protected final StaticFieldHandle fieldHandle;
		protected final StaticFunctionHandle funcHandle;

		FuncHolderType(String typeName, String internalName, FunctionType funcType) {
			super(typeName, internalName, false);
			this.fieldHandle = new StaticFieldHandle("funcField", this, funcType);
			FunctionHandle handle = funcType.getHandle();
			this.funcHandle = new StaticFunctionHandle("invokeDirect", this, handle.getReturnType(), handle.getParamTypeList());
		}

		public StaticFieldHandle getFieldHandle() {
			return this.fieldHandle;
		}

		public StaticFunctionHandle getFuncHandle() {
			return this.funcHandle;
		}
	}

	public static class BoxedPrimitiveType extends DSType {
		private final PrimitiveType type;

		BoxedPrimitiveType(PrimitiveType type) {
			super("$Boxed$_" + type.getTypeName(), type.getInternalName(), false);
			this.type = type;
		}

		public boolean isAcceptableType(PrimitiveType targetType) {
			return this.type.equals(targetType);
		}

		public PrimitiveType getUnwrappedType() {
			return this.type;
		}
	}
}

