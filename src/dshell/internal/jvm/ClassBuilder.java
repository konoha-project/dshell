package dshell.internal.jvm;

import static org.objectweb.asm.Opcodes.V1_6;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.util.LibBunSystem;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

class ClassBuilder {
	final JavaByteCodeGenerator generator;
	final int classQualifer;
	final String sourceFile;
	final String className;
	final String superClassName;
	final ArrayList<MethodNode> methodList = new ArrayList<MethodNode>();
	final ArrayList<FieldNode> fieldList = new ArrayList<FieldNode>();

	ClassBuilder(JavaByteCodeGenerator generator, int classQualifer, String sourceFile, String className, String superClassName) {
		this.generator = generator;
		this.classQualifer = classQualifer;
		this.sourceFile = sourceFile;
		this.className = className;
		this.superClassName = superClassName;
	}

	void addField(int acc, String name, BType zType, Object value) {
		FieldNode fn = new FieldNode(acc, name, Type.getDescriptor(this.generator.getJavaClass(zType)), null, value);
		this.fieldList.add(fn);
	}

	void addField(int acc, String name, Class<?> fieldClass, Object value) {
		FieldNode fn = new FieldNode(acc, name, Type.getDescriptor(fieldClass), null, value);
		this.fieldList.add(fn);
	}

	MethodBuilder newMethodBuilder(int acc, String name, String desc) {
		MethodBuilder methodBuilder = new MethodBuilder(acc, name, desc, this.generator);
		this.methodList.add(methodBuilder);
		return methodBuilder;
	}

	MethodBuilder newMethodBuilder(int acc, String name, BFuncType funcType) {
		return this.newMethodBuilder(acc, name, this.generator.javaTypeUtils.getMethodDescriptor(funcType));
	}

	byte[] generateBytecode() {
		ClassWriter visitor = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		visitor.visit(V1_6, this.classQualifer, this.className, null, this.superClassName, null);
		visitor.visitSource(this.sourceFile, null);
		for(FieldNode f : this.fieldList) {
			f.accept(visitor);
		}
		for(MethodNode m : this.methodList) {
			m.accept(visitor);
		}
		visitor.visitEnd();
		return visitor.toByteArray();
	}


	Method getDefinedMethod(ClassLoader classLoader, String funcName) {
		try {
			Class<?> definedClass = classLoader.loadClass(this.className);
			Method[] definedMethods = definedClass.getMethods();
			for(Method m : definedMethods) {
				if(m.getName().equals(funcName)) {
					return m;
				}
			}
		} catch(Exception e) {
			LibBunSystem._FixMe(e);
		}
		return null;
	}

	void outputClassFile() {  // This is used for debug
		byte[] ba = this.generateBytecode();
		System.err.println("*debug info*: generating " + this.className + ".class");
		System.err.println("*debug info*: check it out with javap -c " + this.className + ".class");
		File file = new File(this.className + ".class");
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(ba);
			fos.close();
		}
		catch(IOException e) {
			System.out.println(e);
		}
	}
}