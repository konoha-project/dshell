package dshell.internal.parser;

import java.util.Collections;
import java.util.List;

import org.objectweb.asm.commons.GeneratorAdapter;

import dshell.internal.parser.TypePool.Type;

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
		assert calleeName != null;
		assert ownerType != null;
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
		private final TypePool.Type fieldType;

		/**
		 * asm type descriptor for owner type.
		 */
		private org.objectweb.asm.Type ownerTypeDesc;

		/**
		 * asm type descriptor for field type.
		 */
		private org.objectweb.asm.Type fieldTypeDesc;

		public FieldHandle(String calleeName, TypePool.Type ownerType, TypePool.Type fieldType) {
			super(calleeName, ownerType);
			assert fieldType != null;
			this.fieldType = fieldType;
		}

		public TypePool.Type getFieldType() {
			return this.fieldType;
		}

		private void initTypeDesc() {
			if(this.ownerTypeDesc == null || this.fieldTypeDesc == null) {
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

		private <T> void initConstructorDesc() {
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
			this.initConstructorDesc();
			adapter.invokeConstructor(this.ownerTypeDesc, methodDesc);
		}
	}

	/**
	 * Represent function.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FunctionHandle extends MethodHandle {
		public FunctionHandle(TypePool.FunctionType funcType, Type returnType, List<Type> paramTypeList) {
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
	 * Represent operator.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class OperatorHandle extends MethodHandle {
		/**
		 * must be fully qualified name.
		 */
		private final String ownerName;

		public OperatorHandle(String calleeName, String ownerName, Type returnType, List<TypePool.Type> paramTypeList) {
			super(calleeName, null, returnType, paramTypeList);
			this.ownerName = ownerName;
		}

		private void initOperatorDesc() {
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
			this.initOperatorDesc();
			adapter.invokeStatic(this.ownerTypeDesc, this.methodDesc);
		}
	}
}
