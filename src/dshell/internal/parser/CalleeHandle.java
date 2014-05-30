package dshell.internal.parser;

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

		protected FieldHandle(String calleeName, TypePool.Type ownerType, TypePool.Type fieldType) {
			super(calleeName, ownerType);
			assert fieldType != null;
			this.fieldType = fieldType;
		}

		public TypePool.Type getFieldType() {
			return this.fieldType;
		}

		private void initTypeDesc() {
			if(this.ownerTypeDesc == null || this.fieldTypeDesc == null) {
				this.ownerTypeDesc = org.objectweb.asm.Type.getType(this.ownerType.getNativeClass());
				this.fieldTypeDesc = org.objectweb.asm.Type.getType(this.fieldType.getNativeClass());
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
		 */
		protected final TypePool.Type[] paramTypes;

		/**
		 * 
		 * @param calleeName
		 * @param ownerType
		 * @param returnType
		 * @param paramTypes
		 * - if has no parameters, it is empty array;
		 */
		protected MethodHandle(String calleeName, TypePool.Type ownerType, TypePool.Type returnType, TypePool.Type[] paramTypes) {
			super(calleeName, ownerType);
			assert returnType != null;
			assert paramTypes != null;
			this.returnType = returnType;
			this.paramTypes = paramTypes;
		}

		public TypePool.Type getReturnType() {
			return this.returnType;
		}

		public TypePool.Type[] getParamTypes() {
			return this.paramTypes;
		}

		protected void initMethodDesc() {
			if(this.ownerTypeDesc == null || this.methodDesc == null) {
				this.ownerTypeDesc = org.objectweb.asm.Type.getType(this.ownerType.getNativeClass());
				int size = this.paramTypes.length;
				Class<?>[] paramClasses = size == 0 ? null : new Class<?>[size];
				for(int i = 0; i < size; i++) {
					paramClasses[i] = this.paramTypes[i].getNativeClass();
				}
				java.lang.reflect.Method method = TypeUtils.getMethod(this.ownerType.getNativeClass(), this.calleeName, paramClasses);
				this.methodDesc = org.objectweb.asm.commons.Method.getMethod(method);
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
		protected ConstructorHandle(TypePool.Type ownerType, TypePool.Type[] paramTypes) {
			super("<init>", ownerType, TypePool.getInstance().voidType, paramTypes);
		}

		private <T> void initConstructorDesc() {
			if(this.ownerTypeDesc == null || this.methodDesc == null) {
				this.ownerTypeDesc = org.objectweb.asm.Type.getType(this.ownerType.getNativeClass());
				int size = this.paramTypes.length;
				Class<?>[] paramClasses = size == 0 ? null : new Class<?>[size];
				for(int i = 0; i < size; i++) {
					paramClasses[i] = this.paramTypes[i].getNativeClass();
				}
				java.lang.reflect.Constructor<T> constructor = TypeUtils.getConstructor(this.ownerType.getNativeClass(), paramClasses);
				this.methodDesc = org.objectweb.asm.commons.Method.getMethod(constructor);
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
		protected FunctionHandle(TypePool.FunctionType funcType) {
			super("invoke", funcType, funcType.getReturnType(), funcType.getParamTypes());
		}

		/**
		 * used for code generation.
		 * generate invokeinterface instruction.
		 */
		@Override
		public void call(GeneratorAdapter adapter) {
			adapter.invokeInterface(this.ownerTypeDesc, this.methodDesc);
		}
	}

	public static class OperatorHandle extends MethodHandle {
		protected OperatorHandle(String calleeName, Type ownerType, Type returnType, Type[] paramTypes) {
			super(calleeName, ownerType, returnType, paramTypes);
		}
	}
}
