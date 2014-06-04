package dshell.internal.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import dshell.internal.lib.DShellClassLoader;
import dshell.internal.parser.CalleeHandle.MethodHandle;
import dshell.internal.parser.CalleeHandle.StaticFunctionHandle;
import dshell.internal.parser.TypePool.ClassType;
import dshell.internal.parser.TypePool.FuncHolderType;
import dshell.internal.parser.TypePool.FunctionType;
import dshell.internal.parser.TypePool.Type;
import dshell.internal.parser.TypeUtils;

/**
 * used for class and function wrapper class generation.
 * @author skgchxngsxyz-osx
 *
 */
public class ClassBuilder extends ClassWriter {
	private static int topLevelClassPrefix = -1;

	private final String internalClassName;

	/**
	 * create new class builder for class generation.
	 * @param classType
	 * - target class type.
	 * @param sourceCode
	 * - class source code, may be null.
	 */
	public ClassBuilder(ClassType classType, String sourceCode) {
		super(ClassWriter.COMPUTE_FRAMES);
		this.internalClassName = classType.getInternalName();
		this.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, this.internalClassName, null, classType.getSuperType().getInternalName(), null);
		this.visitSource(sourceCode, null);
	}

	/**
	 * create new class builder for top level class generation.
	 */
	public ClassBuilder() {
		super(ClassWriter.COMPUTE_FRAMES);
		this.internalClassName = "dshell/defined/toplevel" + ++topLevelClassPrefix;
		this.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, this.internalClassName, null, "java/lang/Object", null);
	}

	/**
	 * create new class builder for function holder class.
	 * @param holderType
	 * - function holder type.
	 * @param sourceCode
	 * function source code, may be null.
	 */
	public ClassBuilder(FuncHolderType holderType, String sourceCode) {
		super(ClassWriter.COMPUTE_FRAMES);
		this.internalClassName = holderType.getInternalName();
		FunctionType superType = (FunctionType) holderType.getFieldHandle().getFieldType();
		this.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, this.internalClassName, null, "java/lang/Object", new String[]{superType.getInternalName()});
		this.visitSource(sourceCode, null);
	}

	/**
	 * create new generator adapter for method generation.
	 * @param handle
	 * - if null, generate adapter for top level wrapper func.
	 * @return
	 */
	public MethodBuilder createNewMethodBuilder(MethodHandle handle) {
		if(handle == null) {
			org.objectweb.asm.commons.Method methodDesc = org.objectweb.asm.commons.Method.getMethod("void invoke()");
			return new MethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, methodDesc, null, null, this);
		}
		if(handle instanceof StaticFunctionHandle) {
			return new MethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, handle.getMethodDesc(), null, null, this);
		}
		return new MethodBuilder(Opcodes.ACC_PUBLIC, handle.getMethodDesc(), null, null, this);
	}

	/**
	 * generate and load class.
	 * must call it only once.
	 * @param classLoader
	 * @return
	 * - generated class.
	 */
	public Class<?> generateClass(DShellClassLoader classLoader) {
		this.visitEnd();
		return classLoader.definedAndLoadClass(this.internalClassName, this.toByteArray());
	}

	@Override
	public String toString() {
		return this.internalClassName;
	}

	public static class MethodBuilder extends GeneratorAdapter {
		protected final Stack<Label> continueLabels;
		protected final Stack<Label> breakLabels;

		protected final LocalVarScopes varScopes;

		protected MethodBuilder(int arg0, Method arg1, String arg2, org.objectweb.asm.Type[] arg3, ClassVisitor arg4) {
			super(arg0, arg1, arg2, arg3, arg4);
			this.continueLabels = new Stack<>();
			this.breakLabels = new Stack<>();
			int startIndex = 0;
			if((arg0 & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) {
				startIndex = 1;
			}
			this.varScopes = new LocalVarScopes(startIndex);
		}

		public Stack<Label> getContinueLabels() {
			return this.continueLabels;
		}

		public Stack<Label> getBreakLabels() {
			return this.breakLabels;
		}

		/**
		 * generate pop instruction.
		 * if type is long or double, generate pop2.
		 * @param type
		 * - stack top type.
		 */
		public void pop(org.objectweb.asm.Type type) {
			if(type.equals(org.objectweb.asm.Type.LONG_TYPE) || type.equals(org.objectweb.asm.Type.DOUBLE_TYPE)) {
				this.pop2();
			} else {
				this.pop();
			}
		}

		public void createNewLocalScope() {
			this.varScopes.createNewScope();
		}

		public void removeCurrentLocalScope() {
			this.varScopes.removeCurrentScope();
		}

		public void defineArgument(String argName, Type argType) {
			assert this.varScopes.scopeDepth() == 2;
			this.varScopes.addLocalVar(argName, argType);
		}

		public void createNewLocalVarAndStoreValue(String varName, Type type) {
			int varIndex = this.varScopes.addLocalVar(varName, type);
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(type);
			this.visitVarInsn(typeDesc.getOpcode(Opcodes.ISTORE), varIndex);
		}

		public void storeValueToLocalVar(String varName, Type type) {
			int varIndex = this.varScopes.getVarIndex(varName);
			if(varIndex == -1) {
				throw new RuntimeException("undefined variable: " + varName);
			}
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(type);
			this.visitVarInsn(typeDesc.getOpcode(Opcodes.ISTORE), varIndex);
		}

		public void loadValueFromLocalVar(String varName, Type type) {
			int varIndex = this.varScopes.getVarIndex(varName);
			if(varIndex == -1) {
				throw new RuntimeException("undefined variable:" + varName);
			}
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(type);
			this.visitVarInsn(typeDesc.getOpcode(Opcodes.ILOAD), varIndex);
		}
	}

	private static class LocalVarScopes {
		/**
		 * contains local variable scopes
		 */
		private final Stack<VarScope> scopes;

		/**
		 * local variable start index.
		 * if this builder represents static method or static initializer, index = 0.
		 * if this builder represents instance method or constructor, index = 1;
		 */
		protected final int startVarIndex;

		private LocalVarScopes(int startIndex) {
			this.scopes = new Stack<>();
			this.scopes.push(new GlobalVarScope());	//FIXME:
			this.startVarIndex = startIndex;
			
		}

		/**
		 * add local variable to scope. 
		 * 
		 * @param varName
		 * - variable name.
		 * @param type
		 * - variable's value type.
		 * @return
		 * - local var index.
		 * throw if variable has already defined in this scope.
		 */
		public int addLocalVar(String varName, Type type) {
			return this.scopes.peek().addLocalVar(varName, type);
		}

		/**
		 * get local variable index.
		 * @param varName
		 * - variable index.
		 * @return
		 * - if has no variable, return -1.
		 */
		public int getVarIndex(String varName) {
			return this.scopes.peek().getLocalVar(varName);
		}

		public void createNewScope() {
			int startIndex = this.startVarIndex;
			if(this.scopes.size() > 1) {
				startIndex = this.scopes.peek().getEndIndex();
			}
			this.scopes.push(new LocalVarScope(this.scopes.peek(), startIndex));
		}

		public void removeCurrentScope() {
			if(this.scopes.size() > 1) {
				this.scopes.pop();
			}
		}

		public int scopeDepth() {
			return this.scopes.size();
		}
	}

	private static interface VarScope {
		/**
		 * add local variable to scope. 
		 * 
		 * @param varName
		 * - variable name.
		 * @param type
		 * - variable's value type.
		 * @return
		 * - local var index.
		 * throw if variable has already defined in this scope.
		 */
		public int addLocalVar(String varName, Type type);

		/**
		 * get local variable index.
		 * @param varName
		 * - variable index.
		 * @return
		 * - if has no variable, return -1.
		 */
		public int getLocalVar(String varName);

		/**
		 * get start index of local variable in this scope.
		 * @return
		 */
		public int getStartIndex();

		/**
		 * get end index of local variable in this scope.
		 * @return
		 */
		public int getEndIndex();
	}

	private static class LocalVarScope implements VarScope {
		/**
		 * parent var scope. may be null if it is root scope.
		 */
		private final VarScope parentScope;

		/**
		 * represent start index of local variable in this scope.
		 */
		private final int localVarBaseIndex;

		/**
		 * represent local variable index.
		 * after adding new local variable, increment this index by value size.
		 */
		private int currentLocalVarIndex;

		/**
		 * contain var index. key is variable name.
		 */
		private final Map<String, Integer> varIndexMap;

		private LocalVarScope(VarScope parentScope, int localVarBaseIndex) {
			this.parentScope = parentScope;
			this.varIndexMap = new HashMap<>();
			this.localVarBaseIndex = localVarBaseIndex;
			this.currentLocalVarIndex = this.localVarBaseIndex;
		}

		@Override
		public int addLocalVar(String varName, Type type) {
			if(this.varIndexMap.containsKey(varName)) {
				throw new RuntimeException(varName + " is already defined");
			}
			int valueSize = TypeUtils.toTypeDescriptor(type).getSize();
			assert valueSize > 0;
			int index = this.currentLocalVarIndex;
			this.varIndexMap.put(varName, index);
			this.currentLocalVarIndex += valueSize;
			return index;
		}

		@Override
		public int getLocalVar(String varName) {
			Integer index = this.varIndexMap.get(varName);
			if(index == null) {
				return this.parentScope.getLocalVar(varName);
			}
			return index;
		}

		@Override
		public int getStartIndex() {
			return this.localVarBaseIndex;
		}

		@Override
		public int getEndIndex() {
			return this.currentLocalVarIndex;
		}
	}

	private static class GlobalVarScope implements VarScope {	// TODO:
		@Override
		public int addLocalVar(String varName, Type type) {
			throw new RuntimeException("unimplemented operation.");
		}

		@Override
		public int getLocalVar(String varName) {
			return -1;
		}

		@Override
		public int getStartIndex() {
			return 0;
		}

		@Override
		public int getEndIndex() {
			return 0;
		}
	}
}
