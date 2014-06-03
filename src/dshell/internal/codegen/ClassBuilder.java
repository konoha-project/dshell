package dshell.internal.codegen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Stack;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import dshell.internal.lib.DShellClassLoader;
import dshell.internal.parser.CalleeHandle.MethodHandle;
import dshell.internal.parser.TypePool.ClassType;

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

		protected MethodBuilder(int arg0, Method arg1, String arg2, Type[] arg3, ClassVisitor arg4) {
			super(arg0, arg1, arg2, arg3, arg4);
			this.continueLabels = new Stack<>();
			this.breakLabels = new Stack<>();
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
	}
}
