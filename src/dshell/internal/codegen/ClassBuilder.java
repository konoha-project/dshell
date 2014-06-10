package dshell.internal.codegen;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import dshell.internal.lib.DShellClassLoader;
import dshell.internal.lib.GlobalVariableTable;
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

	/**
	 * wrapper class of generator adapter
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class MethodBuilder extends GeneratorAdapter {
		protected final Stack<Label> continueLabels;
		protected final Stack<Label> breakLabels;
		protected final Stack<TryBlockLabels> tryLabels;

		protected final VarScopes varScopes;

		protected MethodBuilder(int arg0, Method arg1, String arg2, org.objectweb.asm.Type[] arg3, ClassVisitor arg4) {
			super(arg0, arg1, arg2, arg3, arg4);
			this.continueLabels = new Stack<>();
			this.breakLabels = new Stack<>();
			this.tryLabels = new Stack<>();
			int startIndex = 0;
			if((arg0 & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) {
				startIndex = 1;
			}
			this.varScopes = new VarScopes(startIndex);
		}

		public Stack<Label> getContinueLabels() {
			return this.continueLabels;
		}

		public Stack<Label> getBreakLabels() {
			return this.breakLabels;
		}

		public Stack<TryBlockLabels> getTryLabels() {
			return this.tryLabels;
		}

		public TryBlockLabels createNewTryLabel() {
			return new TryBlockLabels(this);
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
			this.varScopes.addVarEntry(argName, argType);
		}

		public void createNewLocalVarAndStoreValue(String varName, Type type) {
			VarEntry entry = this.varScopes.addVarEntry(varName, type);
			// global variable
			if(entry.isGlobaVar()) {
				this.storeValueToGlobal(entry.getVarIndex(), type);
				return;
			}
			// local variable
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(type);
			this.visitVarInsn(typeDesc.getOpcode(Opcodes.ISTORE), entry.getVarIndex());
			return;
		}

		public void storeValueToLocalVar(String varName, Type type) {
			VarEntry entry = this.varScopes.getVarEntry(varName);
			assert entry != null : "undefined variable: " + varName;
			// global variable
			if(entry.isGlobaVar()) {
				this.storeValueToGlobal(entry.getVarIndex(), type);
				return;
			}
			// local variable
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(type);
			this.visitVarInsn(typeDesc.getOpcode(Opcodes.ISTORE), entry.getVarIndex());
		}

		public void loadValueFromLocalVar(String varName, Type type) {
			VarEntry entry = this.varScopes.getVarEntry(varName);
			assert entry != null : "undefined variable: " + varName;
			// global variable
			if(entry.isGlobaVar()) {
				this.loadValueFromGlobal(entry.getVarIndex(), type);
				return;
			}
			// local variable
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(type);
			this.visitVarInsn(typeDesc.getOpcode(Opcodes.ILOAD), entry.getVarIndex());
		}

		private void storeValueToGlobal(int index, Type type) {
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(type);
			org.objectweb.asm.Type ownerTypeDesc = org.objectweb.asm.Type.getType(GlobalVariableTable.class);
			switch(typeDesc.getSort()) {
			case org.objectweb.asm.Type.LONG:
				this.getStatic(ownerTypeDesc, "longVarTable", org.objectweb.asm.Type.getType(long[].class));
				this.push(index);
				this.dup2X2();
				this.pop2();
				this.arrayStore(typeDesc);
				break;
			case org.objectweb.asm.Type.DOUBLE:
				this.getStatic(ownerTypeDesc, "doubleVarTable", org.objectweb.asm.Type.getType(double[].class));
				this.push(index);
				this.dup2X2();
				this.pop2();
				this.arrayStore(typeDesc);
				break;
			case org.objectweb.asm.Type.BOOLEAN:
				this.getStatic(ownerTypeDesc, "booleanVarTable", org.objectweb.asm.Type.getType(boolean[].class));
				this.swap();
				this.push(index);
				this.swap();
				this.arrayStore(typeDesc);
				break;
			case org.objectweb.asm.Type.OBJECT:
				this.getStatic(ownerTypeDesc, "objectVarTable", org.objectweb.asm.Type.getType(Object[].class));
				this.swap();
				this.push(index);
				this.swap();
				this.arrayStore(typeDesc);
				break;
			default:
				throw new RuntimeException("illegal type: " + type);
			}
		}

		private void loadValueFromGlobal(int index, Type type) {
			org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(type);
			org.objectweb.asm.Type ownerTypeDesc = org.objectweb.asm.Type.getType(GlobalVariableTable.class);
			switch(typeDesc.getSort()) {
			case org.objectweb.asm.Type.LONG:
				this.getStatic(ownerTypeDesc, "longVarTable", org.objectweb.asm.Type.getType(long[].class));
				this.push(index);
				this.arrayLoad(typeDesc);
				break;
			case org.objectweb.asm.Type.DOUBLE:
				this.getStatic(ownerTypeDesc, "doubleVarTable", org.objectweb.asm.Type.getType(double[].class));
				this.push(index);
				this.arrayLoad(typeDesc);
				break;
			case org.objectweb.asm.Type.BOOLEAN:
				this.getStatic(ownerTypeDesc, "booleanVarTable", org.objectweb.asm.Type.getType(boolean[].class));
				this.push(index);
				this.arrayLoad(typeDesc);
				break;
			case org.objectweb.asm.Type.OBJECT:
				this.getStatic(ownerTypeDesc, "objectVarTable", org.objectweb.asm.Type.getType(Object[].class));
				this.push(index);
				this.arrayLoad(org.objectweb.asm.Type.getType(Object.class));
				this.visitTypeInsn(Opcodes.CHECKCAST, typeDesc.getInternalName());
				break;
			default:
				throw new RuntimeException("illegal type: " + type);
			}
		}
	}

	public static class TryBlockLabels {
		public final Label startLabel;
		public final Label endLabel;
		public final Label finallyLabel;

		private TryBlockLabels(GeneratorAdapter adapter) {
			this.startLabel = adapter.newLabel();
			this.endLabel = adapter.newLabel();
			this.finallyLabel = adapter.newLabel();
		}
	}

	private static class VarScopes {
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

		private VarScopes(int startIndex) {
			this.scopes = new Stack<>();
			this.scopes.push(GlobalVarScope.getInstance());
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
		public VarEntry addVarEntry(String varName, Type type) {
			return this.scopes.peek().addVarEntry(varName, type);
		}

		/**
		 * get local variable index.
		 * @param varName
		 * - variable index.
		 * @return
		 * - if has no variable, return null.
		 */
		public VarEntry getVarEntry(String varName) {
			return this.scopes.peek().getVarEntry(varName);
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
		public VarEntry addVarEntry(String varName, Type type);

		/**
		 * get local variable index.
		 * @param varName
		 * - variable index.
		 * @return
		 * - if has no var entry, return null.
		 */
		public VarEntry getVarEntry(String varName);

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
		 * contain var entry. key is variable name.
		 */
		private final Map<String, VarEntry> varEntryMap;

		private LocalVarScope(VarScope parentScope, int localVarBaseIndex) {
			this.parentScope = parentScope;
			this.varEntryMap = new HashMap<>();
			this.localVarBaseIndex = localVarBaseIndex;
			this.currentLocalVarIndex = this.localVarBaseIndex;
		}

		@Override
		public VarEntry addVarEntry(String varName, Type type) {
			assert this.varEntryMap.containsKey(varName) : varName + " is already defined";
			int valueSize = TypeUtils.toTypeDescriptor(type).getSize();
			assert valueSize > 0;
			int index = this.currentLocalVarIndex;
			VarEntry entry = new VarEntry(index, false);
			this.varEntryMap.put(varName, entry);
			this.currentLocalVarIndex += valueSize;
			return entry;
		}

		@Override
		public VarEntry getVarEntry(String varName) {
			VarEntry entry = this.varEntryMap.get(varName);
			if(entry == null) {
				return this.parentScope.getVarEntry(varName);
			}
			return entry;
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

	/**
	 * contains global variable entry
	 * @author skgchxngsxyz-osx
	 *
	 */
	private static class GlobalVarScope implements VarScope {
		private final Map<String, VarEntry> globalVarEntryMap;

		private GlobalVarScope() {
			this.globalVarEntryMap = new HashMap<>();
		}

		@Override
		public VarEntry addVarEntry(String varName, Type type) {
			assert !this.globalVarEntryMap.containsKey(varName) : varName + " is already defined";
			int varIndex = -1;
			switch(TypeUtils.toTypeDescriptor(type).getSort()) {
			case org.objectweb.asm.Type.LONG:
				varIndex = GlobalVariableTable.reserveLongVarTable();
				break;
			case org.objectweb.asm.Type.DOUBLE:
				varIndex = GlobalVariableTable.reserveDoubleVarTable();
				break;
			case org.objectweb.asm.Type.BOOLEAN:
				varIndex = GlobalVariableTable.reserveBooleanVarTable();
				break;
			case org.objectweb.asm.Type.OBJECT:
				varIndex = GlobalVariableTable.reserveObjectVarTable();
				break;
			default:
				throw new RuntimeException("illegal type: " + type);
			}
			assert varIndex != -1;
			VarEntry entry = new VarEntry(varIndex, true);
			this.globalVarEntryMap.put(varName, entry);
			return entry;
		}

		@Override
		public VarEntry getVarEntry(String varName) {
			return this.globalVarEntryMap.get(varName);
		}

		@Override
		public int getStartIndex() {
			return 0;
		}

		@Override
		public int getEndIndex() {
			return 0;
		}

		private static class Holder {
			private final static GlobalVarScope INSTANCE = new GlobalVarScope();
		}

		public static GlobalVarScope getInstance() {
			return Holder.INSTANCE;
		}
	}

	/**
	 * contains var index and var flag(isGlobal)
	 * @author skgchxngsxyz-osx
	 *
	 */
	private static class VarEntry {
		/**
		 * In local variable, this index represents jvm local variable table' s index.
		 * In global variable, represents global var table index.
		 */
		private final int varIndex;

		/**
		 * represent variable scope.
		 * if true, variable is global variable.
		 */
		private final boolean isGlobal;

		private VarEntry(int varIndex, boolean isGlobal) {
			this.varIndex = varIndex;
			this.isGlobal = isGlobal;
		}

		private int getVarIndex() {
			return this.varIndex;
		}

		private boolean isGlobaVar() {
			return this.isGlobal;
		}
 	}
}
