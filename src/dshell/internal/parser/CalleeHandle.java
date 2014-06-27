package dshell.internal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.commons.GeneratorAdapter;

import dshell.internal.parser.TypePool.GenericType;
import dshell.internal.parser.TypePool.ParametricType;
import dshell.internal.parser.TypePool.PrimitiveType;

/**
 * Represents method or instance field.
 * Used for type checker and code generator.
 * @author skgchxngsxyz-osx
 *
 */
public abstract class CalleeHandle {
	/**
	 * name of callee method.
	 */
	protected final String calleeName;

	/**
	 * Represents callee owner type.
	 * instance method's receiver is the same as it.
	 */
	protected final TypePool.Type ownerType;

	public String getCalleeName() {
		return calleeName;
	}

	public TypePool.Type getOwnerType() {
		return ownerType;
	}

	protected CalleeHandle(String calleeName, TypePool.Type ownerType) {
		this.calleeName = calleeName;
		this.ownerType = ownerType;
	}

	/**
	 * Represent instance field. 
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FieldHandle extends CalleeHandle {
		/**
		 * Represent instance field type.
		 */
		protected final TypePool.Type fieldType;

		/**
		 * asm type descriptor for owner type.
		 */
		protected org.objectweb.asm.Type ownerTypeDesc;

		/**
		 * asm type descriptor for field type.
		 */
		protected org.objectweb.asm.Type fieldTypeDesc;

		public FieldHandle(String calleeName, TypePool.Type ownerType, TypePool.Type fieldType) {
			super(calleeName, ownerType);
			assert fieldType != null;
			this.fieldType = fieldType;
		}

		public TypePool.Type getFieldType() {
			return this.fieldType;
		}

		protected void initTypeDesc() {
			if(this.ownerTypeDesc == null || this.fieldTypeDesc == null) {
				assert this.ownerType != null;
				assert this.fieldType != null;
				this.ownerTypeDesc = TypeUtils.toTypeDescriptor(this.ownerType);
				this.fieldTypeDesc = TypeUtils.toTypeDescriptor(this.fieldType);
			}
		}

		/**
		 * used for code generator.
		 * generate getField instruction.
		 * @param adapter
		 */
		public void callGetter(GeneratorAdapter adapter) {
			assert adapter != null;
			this.initTypeDesc();
			adapter.getField(this.ownerTypeDesc, this.calleeName, this.fieldTypeDesc);
		}

		/**
		 * used for code generator.
		 * generate putField instruction.
		 * @param adapter
		 */
		public void callSetter(GeneratorAdapter adapter) {
			assert adapter != null;
			this.initTypeDesc();
			adapter.putField(this.ownerTypeDesc, this.calleeName, this.fieldTypeDesc);
		}
	}

	public static class StaticFieldHandle extends FieldHandle {
		public StaticFieldHandle(String calleeName, TypePool.Type ownerType, TypePool.Type fieldType) {
			super(calleeName, ownerType, fieldType);
		}

		@Override
		public void callGetter(GeneratorAdapter adapter) {
			this.initTypeDesc();
			adapter.getStatic(this.ownerTypeDesc, this.calleeName, this.fieldTypeDesc);
		}

		@Override
		public void callSetter(GeneratorAdapter adapter) {
			this.initTypeDesc();
			adapter.putStatic(this.ownerTypeDesc, this.calleeName, this.fieldTypeDesc);
		}
	}
	/**
	 * Represent instance method.
	 * It contains return type, param types, and jvm method descriptor.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class MethodHandle extends CalleeHandle {
		/**
		 * for jvm method invoke only.
		 * initialized when it needs.
		 */
		protected org.objectweb.asm.commons.Method methodDesc;

		/**
		 * for jvm method invoke only.
		 * initialized when it needs.
		 */
		protected org.objectweb.asm.Type ownerTypeDesc;

		protected final TypePool.Type returnType;

		/**
		 * not contains receiver type.
		 * it is unmodified.
		 */
		protected final List<TypePool.Type> paramTypeList;

		/**
		 * 
		 * @param calleeName
		 * @param ownerType
		 * @param returnType
		 * @param paramTypes
		 * - if has no parameters, it is empty array;
		 */
		public MethodHandle(String calleeName, TypePool.Type ownerType, TypePool.Type returnType, List<TypePool.Type> paramTypeList) {
			super(calleeName, ownerType);
			assert returnType != null;
			assert paramTypeList != null;
			this.returnType = returnType;
			this.paramTypeList = Collections.unmodifiableList(paramTypeList);
		}

		public TypePool.Type getReturnType() {
			return this.returnType;
		}

		public List<TypePool.Type> getParamTypeList() {
			return this.paramTypeList;
		}

		protected void initMethodDesc() {
			if(this.ownerTypeDesc == null || this.methodDesc == null) {
				this.ownerTypeDesc = TypeUtils.toTypeDescriptor(this.ownerType);
				this.methodDesc = TypeUtils.toMehtodDescriptor(this.returnType, this.calleeName, this.paramTypeList);
			}
		}

		/**
		 * get method descriptor for method generation.
		 * @return
		 */
		public org.objectweb.asm.commons.Method getMethodDesc() {
			this.initMethodDesc();
			return this.methodDesc;
		}

		/**
		 * used for code generation.
		 * generation invoke virtual instruction
		 * @param adapter
		 */
		public void call(GeneratorAdapter adapter) {
			this.initMethodDesc();
			adapter.invokeVirtual(this.ownerTypeDesc, this.methodDesc);
		}
	}

	/**
	 * Represent constructor.
	 * return type is always void.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ConstructorHandle extends MethodHandle {
		public ConstructorHandle(TypePool.Type ownerType, List<TypePool.Type> paramTypeList) {
			super("<init>", ownerType, TypePool.voidType, paramTypeList);
		}

		@Override
		protected void initMethodDesc() {
			if(this.ownerTypeDesc == null || this.methodDesc == null) {
				this.ownerTypeDesc = TypeUtils.toTypeDescriptor(this.ownerType);
				this.methodDesc = TypeUtils.toConstructorDescriptor(this.paramTypeList);
			}
		}

		/**
		 * used for code generation.
		 * generate invokespecial instruction.
		 * only call constructor (do not generation new instruction.)
		 */
		@Override
		public void call(GeneratorAdapter adapter) {
			this.initMethodDesc();
			adapter.invokeConstructor(this.ownerTypeDesc, methodDesc);
		}
	}

	/**
	 * Represent function.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FunctionHandle extends MethodHandle {
		public FunctionHandle(TypePool.FunctionType funcType, TypePool.Type returnType, List<TypePool.Type> paramTypeList) {
			super("invoke", funcType, returnType, paramTypeList);
		}

		/**
		 * used for code generation.
		 * generate invokeinterface instruction.
		 */
		@Override
		public void call(GeneratorAdapter adapter) {
			this.initMethodDesc();
			adapter.invokeInterface(this.ownerTypeDesc, this.methodDesc);
		}
	}

	/**
	 * represent static function.
	 * @author skgchxngsxyz-opensuse
	 *
	 */
	public static class StaticFunctionHandle extends MethodHandle {
		public StaticFunctionHandle(String calleeName, TypePool.Type ownerType, TypePool.Type returnType, List<TypePool.Type> paramTypeList) {
			super(calleeName, ownerType, returnType, paramTypeList);
		}

		/**
		 * used for code generation.
		 * generate invokestatic instruction.
		 */
		@Override
		public void call(GeneratorAdapter adapter) {
			this.initMethodDesc();
			adapter.invokeStatic(this.ownerTypeDesc, this.methodDesc);
		}
	}

	/**
	 * Represent operator.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class OperatorHandle extends MethodHandle {
		/**
		 * must be fully qualified name.
		 */
		private final String ownerName;

		public OperatorHandle(String calleeName, String ownerName, TypePool.Type returnType, List<TypePool.Type> paramTypeList) {
			super(calleeName, null, returnType, paramTypeList);
			this.ownerName = ownerName;
		}

		@Override
		protected void initMethodDesc() {
			if(this.ownerTypeDesc == null || this.methodDesc == null) {
				this.ownerTypeDesc = TypeUtils.toTypeDescriptor(this.ownerName);
				this.methodDesc = TypeUtils.toMehtodDescriptor(this.returnType, this.calleeName, this.paramTypeList);
			}
		}

		/**
		 * used for code generation.
		 * generate invokestatic instruction.
		 */
		@Override
		public void call(GeneratorAdapter adapter) {
			this.initMethodDesc();
			adapter.invokeStatic(this.ownerTypeDesc, this.methodDesc);
		}
	}

	// callee handle for generic type
	public static class ReifiedFieldHandle extends FieldHandle {
		private final FieldHandle baseHandle;

		private ReifiedFieldHandle(FieldHandle baseHandle, GenericType ownerType, TypePool.Type fieldType) {
			super(baseHandle.getCalleeName(), ownerType, fieldType);
			this.baseHandle = baseHandle;
		}

		@Override
		public void callGetter(GeneratorAdapter adapter) {
			this.baseHandle.callGetter(adapter);
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(this.fieldType);
			if(this.fieldType instanceof PrimitiveType) {
				adapter.box(typeDesc);
			} else {
				adapter.checkCast(typeDesc);
			}
		}

		@Override
		public void callSetter(GeneratorAdapter adapter) {
			this.baseHandle.callSetter(adapter);
		}

		public static FieldHandle createReifiedHandle(FieldHandle baseHandle, GenericType ownerType, List<TypePool.Type> elementTypeList) {
			TypePool.Type newFieldType = replaceParametricType(baseHandle.getFieldType(), elementTypeList);
			return new ReifiedFieldHandle(baseHandle, ownerType, newFieldType);
		}
	}

	public static class ReifiedMethodHandle extends MethodHandle {
		private final MethodHandle baseHandle;

		private ReifiedMethodHandle(MethodHandle baseHandle, GenericType ownerType, TypePool.Type returnType, List<TypePool.Type> paramTypeList) {
			super(baseHandle.getCalleeName(), ownerType, returnType, paramTypeList);
			this.baseHandle = baseHandle;
		}

		@Override
		public void call(GeneratorAdapter adapter) {
			this.baseHandle.call(adapter);
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(this.returnType);
			if(this.returnType instanceof PrimitiveType) {
				adapter.box(typeDesc);
			} else {
				adapter.checkCast(typeDesc);
			}
		}

		public static MethodHandle createReifiedHandle(MethodHandle baseHandle, GenericType ownerType, List<TypePool.Type> elementTypeList) {
			List<TypePool.Type> newParamTypeList = new ArrayList<TypePool.Type>(baseHandle.getParamTypeList().size());
			for(TypePool.Type paramType : baseHandle.getParamTypeList()) {
				newParamTypeList.add(replaceParametricType(paramType, elementTypeList));
			}
			TypePool.Type newReturnType = replaceParametricType(baseHandle.getReturnType(), elementTypeList);
			return new ReifiedMethodHandle(baseHandle, ownerType, newReturnType, newParamTypeList);
		}
	}

	public static class ReifiedConstructorHandle extends ConstructorHandle {
		private final ConstructorHandle baseHandle;
		private ReifiedConstructorHandle(ConstructorHandle baseHandle, GenericType ownerType, List<TypePool.Type> paramTypeList) {
			super(ownerType, paramTypeList);
			this.baseHandle = baseHandle;
		}

		@Override
		public void call(GeneratorAdapter adapter) {
			this.baseHandle.call(adapter);
		}

		public static ConstructorHandle createReifiedHandle(ConstructorHandle baseHandle, GenericType ownerType, List<TypePool.Type> elementTypeList) {
			List<TypePool.Type> newParamTypeList = new ArrayList<TypePool.Type>(baseHandle.getParamTypeList().size());
			for(TypePool.Type paramType : baseHandle.getParamTypeList()) {
				newParamTypeList.add(replaceParametricType(paramType, elementTypeList));
			}
			return new ReifiedConstructorHandle(baseHandle, ownerType, newParamTypeList);
		}
	}

	private static TypePool.Type replaceParametricType(TypePool.Type type, List<TypePool.Type> elementTypeList) {
		if(type instanceof ParametricType) {
			return elementTypeList.get(((ParametricType) type).getParamId());
		}
		return type;
	}
}
