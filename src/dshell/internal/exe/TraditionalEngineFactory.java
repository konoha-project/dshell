package dshell.internal.exe;

import java.lang.reflect.Constructor;

import libbun.parser.classic.LibBunTypeChecker;
import dshell.internal.grammar.DShellGrammar;
import dshell.internal.jvm.JavaByteCodeGenerator;
import dshell.internal.lang.DShellTypeChecker;
import dshell.internal.lib.Utils;

public class TraditionalEngineFactory implements EngineFactory {
	protected final Class<?> generatorClass;
	protected final Class<?> typeCheckerClass;
	protected boolean called;

	public TraditionalEngineFactory() {
		this(JavaByteCodeGenerator.class, DShellTypeChecker.class);
	}

	public TraditionalEngineFactory(Class<?> generatorClass, Class<?> typeCheckerClass) {
		this.generatorClass = generatorClass;
		this.typeCheckerClass = typeCheckerClass;
		this.called = false;
	}

	@Override
	public ExecutionEngine getEngine() {
		if(this.called) {
			throw new RuntimeException("already called");
		}
		this.called = true;
		JavaByteCodeGenerator generator = this.newGenerator();
		DShellGrammar.ImportGrammar(generator.RootGamma);
		generator.SetTypeChecker(newTypeChecker(generator));
		generator.RequireLibrary("common", null);
		return new TraditionalExecutionEngine(generator);
	}

	protected JavaByteCodeGenerator newGenerator() {
		try {
			return (JavaByteCodeGenerator) this.generatorClass.newInstance();
		}
		catch(Exception e) {
			e.printStackTrace();
			Utils.fatal(1, "cannot loading generator: " + generatorClass.getSimpleName());
		}
		return null;
	}

	protected LibBunTypeChecker newTypeChecker(JavaByteCodeGenerator generator) {
		try {
			Constructor<?> constructor = this.typeCheckerClass.getConstructor(JavaByteCodeGenerator.class);
			DShellTypeChecker typeChecker = (DShellTypeChecker) constructor.newInstance(new Object[] {generator});
			return typeChecker;
		}
		catch(Exception e) {
			e.printStackTrace();
			Utils.fatal(1, "cannot loading typechecker: " + typeCheckerClass.getSimpleName());
		}
		return null;
	}
}
