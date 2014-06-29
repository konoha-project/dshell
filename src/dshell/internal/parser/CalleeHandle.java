package dshell.internal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.commons.GeneratorAdapter;

import dshell.internal.type.ReifiedType;
import dshell.internal.type.DSType;
import dshell.internal.type.DSType.FunctionType;
import dshell.internal.type.DSType.ParametricType;
import dshell.internal.type.DSType.PrimitiveType;
import dshell.internal.type.TypePool;


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
	protected final DSType ownerType;

	public String getCalleeName() {
		return calleeName;
	}

	public DSType getOwnerType() {
		return ownerType;
	}

	protected CalleeHandle(String calleeName, DSType ownerType) {
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
		protected final DSType fieldType;

		/**
		 * asm type descriptor for owner type.
		 */
		protected org.objectweb.asm.Type ownerTypeDesc;

		/**
		 * asm type descriptor for field type.
		 */
		protected org.objectweb.asm.Type fieldTypeDesc;

		public FieldHandle(String calleeName, DSType ownerType, DSType fieldType) {
			super(calleeName, ownerType);
			assert fieldType != null;
			this.fieldType = fieldType;
		}

		public DSType getFieldType() {
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
		public StaticFieldHandle(String calleeName, DSType ownerType, DSType fieldType) {
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

		protected final DSType returnType;

		/**
		 * not contains receiver type.
		 * it is unmodified.
		 */
		protected final List<DSType> paramTypeList;

		/**
		 * 
		 * @param calleeName
		 * @param ownerType
		 * @param returnType
		 * @param paramTypes
		 * - if has no parameters, it is empty array;
		 */
		public MethodHandle(String calleeName, DSType ownerType, DSType returnType, List<DSType> paramTypeList) {
			super(calleeName, ownerType);
			assert returnType != null;
			assert paramTypeList != null;
			this.returnType = returnType;
			this.paramTypeList = Collections.unmodifiableList(paramTypeList);
		}

		public DSType getReturnType() {
			return this.returnType;
		}

		public List<DSType> getParamTypeList() {
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
		public ConstructorHandle(DSType ownerType, List<DSType> paramTypeList) {
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
		public FunctionHandle(FunctionType funcType, DSType returnType, List<DSType> paramTypeList) {
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
		public StaticFunctionHandle(String calleeName, DSType ownerType, DSType returnType, List<DSType> paramTypeList) {
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

		public OperatorHandle(String calleeName, String ownerName, DSType returnType, List<DSType> paramTypeList) {
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

		private ReifiedFieldHandle(FieldHandle baseHandle, ReifiedType ownerType, DSType fieldType) {
			super(baseHandle.getCalleeName(), ownerType, fieldType);
			this.baseHandle = baseHandle;
		}

		@Override
		public void callGetter(GeneratorAdapter adapter) {
			this.baseHandle.callGetter(adapter);
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(this.fieldType);
			if(this.fieldType instanceof PrimitiveType) {
				adapter.unbox(typeDesc);
			} else {
				adapter.checkCast(typeDesc);
			}
		}

		@Override
		public void callSetter(GeneratorAdapter adapter) {
			this.baseHandle.callSetter(adapter);
		}

		public static FieldHandle createReifiedHandle(FieldHandle baseHandle, ReifiedType ownerType, List<DSType> elementTypeList) {
			DSType newFieldType = replaceParametricType(baseHandle.getFieldType(), elementTypeList);
			return new ReifiedFieldHandle(baseHandle, ownerType, newFieldType);
		}
	}

	public static class ReifiedMethodHandle extends MethodHandle {
		private final MethodHandle baseHandle;

		private ReifiedMethodHandle(MethodHandle baseHandle, ReifiedType ownerType, DSType returnType, List<DSType> paramTypeList) {
			super(baseHandle.getCalleeName(), ownerType, returnType, paramTypeList);
			this.baseHandle = baseHandle;
		}

		@Override
		public void call(GeneratorAdapter adapter) {
			this.baseHandle.call(adapter);
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(this.returnType);
			if(this.returnType instanceof PrimitiveType) {
				adapter.unbox(typeDesc);
			} else {
				adapter.checkCast(typeDesc);
			}
		}

		public static MethodHandle createReifiedHandle(MethodHandle baseHandle, ReifiedType ownerType, List<DSType> elementTypeList) {
			List<DSType> newParamTypeList = new ArrayList<DSType>(baseHandle.getParamTypeList().size());
			for(DSType paramType : baseHandle.getParamTypeList()) {
				newParamTypeList.add(replaceParametricType(paramType, elementTypeList));
			}
			DSType newReturnType = replaceParametricType(baseHandle.getReturnType(), elementTypeList);
			return new ReifiedMethodHandle(baseHandle, ownerType, newReturnType, newParamTypeList);
		}
	}

	public static class ReifiedConstructorHandle extends ConstructorHandle {
		private final ConstructorHandle baseHandle;
		private ReifiedConstructorHandle(ConstructorHandle baseHandle, ReifiedType ownerType, List<DSType> paramTypeList) {
			super(ownerType, paramTypeList);
			this.baseHandle = baseHandle;
		}

		@Override
		public void call(GeneratorAdapter adapter) {
			this.baseHandle.call(adapter);
		}

		public static ConstructorHandle createReifiedHandle(ConstructorHandle baseHandle, ReifiedType ownerType, List<DSType> elementTypeList) {
			List<DSType> newParamTypeList = new ArrayList<DSType>(baseHandle.getParamTypeList().size());
			for(DSType paramType : baseHandle.getParamTypeList()) {
				newParamTypeList.add(replaceParametricType(paramType, elementTypeList));
			}
			return new ReifiedConstructorHandle(baseHandle, ownerType, newParamTypeList);
		}
	}

	private static DSType replaceParametricType(DSType type, List<DSType> elementTypeList) {
		if(type instanceof ParametricType) {
			return elementTypeList.get(((ParametricType) type).getParamId());
		}
		return type;
	}
}
