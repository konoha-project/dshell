package dshell.internal.jvm;

import java.util.HashMap;

import libbun.ast.BNode;
import libbun.util.LibBunSystem;

import org.objectweb.asm.Type;

class GeneratedClassLoader extends ClassLoader {
	protected final HashMap<String,ClassBuilder> classBuilderMap;
	private final JavaByteCodeGenerator generator;

	public GeneratedClassLoader(JavaByteCodeGenerator generator) {
		this.generator = generator;
		this.classBuilderMap = new HashMap<String, ClassBuilder>();
	}

	ClassBuilder newClassBuilder(int classQualifer, BNode node, String className, String superClass) {
		String sourceFile = null;
		if(node != null && node.SourceToken != null) {
			sourceFile = node.SourceToken.GetFileName();
		}
		ClassBuilder classBuilder = new ClassBuilder(this.generator, classQualifer, sourceFile, className, superClass);
		this.classBuilderMap.put(className, classBuilder);
		return classBuilder;
	}

	ClassBuilder newClassBuilder(int classQualifer, BNode node, String className, Class<?> superClass) {
		return this.newClassBuilder(classQualifer, node, className, Type.getInternalName(superClass));
	}

	@Override protected Class<?> findClass(String name) {
		ClassBuilder classBuilder = this.classBuilderMap.get(name);
		if(classBuilder != null) {
			byte[] b = classBuilder.generateBytecode();
			if(LibBunSystem.DebugMode) {
				classBuilder.outputClassFile();
			}
			this.classBuilderMap.remove(name);
			try {
				return this.defineClass(name, b, 0, b.length);
			}
			catch(Error e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		return null;
	}

	public Class<?> loadGeneratedClass(String className) {
		try {
			return this.loadClass(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			LibBunSystem._Exit(1, "generation failed: " + className);
		}
		return null;
	}
}