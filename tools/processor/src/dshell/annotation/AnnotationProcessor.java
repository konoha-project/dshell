package dshell.annotation;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;
import javax.tools.Diagnostic.Kind;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({"dshell.annotation.Shared", "dshell.annotation.OpType",
	"dshell.annotation.Wrapper", "dshell.annotation.OpHolder", "dshell.annotation.WrapperClass", 
	"dshell.annotation.SharedClass"})
public class AnnotationProcessor extends AbstractProcessor {
	private final static String A_SHARED_CLASS = "dshell.annotation.SharedClass";
	private final static String A_WRAPPER_CLASS = "dshell.annotation.WrapperClass";
	private final static String A_OP_HOLDER = "dshell.annotation.OpHolder";

	private Map<String, String> typeMap;
	private boolean isInitialazed = false;

	private void initialize() {
		this.typeMap = new HashMap<>();
		this.typeMap.put("long", "pool.intType");
		this.typeMap.put("double", "pool.floatType");
		this.typeMap.put("boolean", "pool.booleanType");
		this.typeMap.put("java.lang.String", "pool.stringType");
		this.typeMap.put("String", "pool.stringType");
		this.typeMap.put("java.lang.Object", "pool.objectType");
		this.typeMap.put("Object", "pool.objectType");
		this.typeMap.put("void", "pool.voidType");
	}

	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		this.debugPrint("call processor");
		if(this.isInitialazed) {
			return true;
		}
		this.isInitialazed = true;
		this.initialize();
		for(TypeElement annotation : annotations) {
			if(this.matchAnnotation(annotation, A_OP_HOLDER)) {
				Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
				if(elements.size() != 1) {
					this.reportErrorAndExit("found duplicated operator table", annotation);
				}
				for(Element element : elements) {
					this.generateOpTable(element);
				}
			} else if(this.matchAnnotation(annotation, A_WRAPPER_CLASS)) {	//TODO: support multiple wrapper class
				Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
				if(elements.size() != 1) {
					this.reportErrorAndExit("found duplicated wrapper class", annotation);
				}
				for(Element element : elements) {
					this.generateWrapperClass(element);
				}
			} else if(this.matchAnnotation(annotation, A_SHARED_CLASS)) {
				Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
				for(Element element : elements) {
					this.generateTypeInitClass(element);
				}
			}
		}
		return true;
	}

	/**
	 * match annotation 
	 * @param annotation
	 * @param targetName
	 * @return
	 * - if annotaion's name equals targetName, return true.
	 */
	private boolean matchAnnotation(TypeElement annotation, String targetName) {
		this.debugPrint("target name-> " + targetName);
		if(annotation.getKind() != ElementKind.ANNOTATION_TYPE) {
			return false;
		}
		String name = annotation.getQualifiedName().toString();
		this.debugPrint("annotation name-> " + name);
		return name.equals(targetName);
	}

	/**
	 * report error and exit processing.
	 * @param message
	 * @param element
	 */
	private void reportErrorAndExit(String message, Element element) {
		this.processingEnv.getMessager().printMessage(Kind.ERROR, message, element);
	}

	/**
	 * report warning.
	 * @param message
	 * @param element
	 */
	private void reportWarning(String message, Element element) {
		this.processingEnv.getMessager().printMessage(Kind.WARNING, message, element);
	}

	private void debugPrint(String message) {
		this.processingEnv.getMessager().printMessage(Kind.NOTE, "DEBUG: " + message);
	}

	private String toTypeName(VariableElement variableElement) {
		String name = variableElement.asType().toString();
		String typeName = this.typeMap.get(name);
		if(typeName == null) {
			this.reportErrorAndExit("unsupported type: " + name, variableElement);
			return null;
		}
		return typeName;
	}

	private String toReturnTypeName(ExecutableElement element) {
		String name = element.getReturnType().toString();
		String typeName = this.typeMap.get(name);
		if(typeName == null) {
			this.reportErrorAndExit("unsupported  type: " + name, element);
			return null;
		}
		return typeName;
	}

	/**
	 * generate OpTable
	 * @param element
	 * - TypeElement having OpHolder annotation
	 */
	private void generateOpTable(Element element) {
		OpTableBuilder builder = new OpTableBuilder();
		TypeElement typeElement = (TypeElement) element;
		List<ExecutableElement> methodElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
		for(ExecutableElement e : methodElements) {
			OpType anno = e.getAnnotation(OpType.class);
			if(anno != null) {
				this.generateOperator(builder, e, anno);
			}
		}
		builder.writeToFile(this.processingEnv.getFiler());
	}

	/**
	 * 
	 * @param builder
	 * @param element
	 * - ExecutableElement having OpType annotation
	 * @param anno
	 */
	private void generateOperator(OpTableBuilder builder, ExecutableElement element, OpType anno) {
		String op = anno.value().getOpSymbol();
		if(op.equals("")) {
			this.reportWarning("value is undefined", element);
			return;
		}
		String methodName = element.getSimpleName().toString();
		@SuppressWarnings("unchecked")
		List<VariableElement> varElements = (List<VariableElement>) element.getParameters();
		int size = varElements.size();
		String[] paramTypeNames = new String[size];
		for(int i = 0; i < size; i++) {
			paramTypeNames[i] = this.toTypeName(varElements.get(i));
		}
		String returnTypeName = this.toReturnTypeName(element);
		builder.appendLine(returnTypeName, op, methodName, paramTypeNames);
	}

	/**
	 * generate wrapper class
	 * @param element
	 * - TypeElement having WrapperClass annotation
	 */
	private void generateWrapperClass(Element element) {
		StringWrapperBuilder builder = new StringWrapperBuilder();
		TypeElement typeElement = (TypeElement) element;
		List<ExecutableElement> methodElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
		for(ExecutableElement e : methodElements) {
			Wrapper anno = e.getAnnotation(Wrapper.class);
			if(anno != null) {
				this.generateWrapper(builder, e);
			}
		}
		builder.writeToFile(this.processingEnv.getFiler());
	}

	/**
	 * 
	 * @param builder
	 * @param element
	 * - ExecutabelElement having Wrapper annotation
	 */
	private void generateWrapper(StringWrapperBuilder builder, ExecutableElement element) {
		String methodName = element.getSimpleName().toString();
		@SuppressWarnings("unchecked")
		List<VariableElement> varElements = (List<VariableElement>) element.getParameters();
		int size = varElements.size() - 1;
		String[] paramTypeNames = new String[size];
		for(int i = 0; i < size; i++) {
			paramTypeNames[i] = this.toTypeName(varElements.get(i + 1));
		}
		String returnTypeName = this.toReturnTypeName(element);
		builder.appendLine(returnTypeName, methodName, paramTypeNames);
	}

	/**
	 * generate TypeInit class
	 * @param element
	 * - TypeElement having SharedClass annotation.
	 */
	private void generateTypeInitClass(Element element) {
		if(element.getAnnotation(GenericClass.class) != null) {	//TODO: generic type support.
			return;
		}
		TypeElement typeElement = (TypeElement) element;
		TypeInitBuilder builder = new TypeInitBuilder(typeElement.getSimpleName().toString());
		List<ExecutableElement> methodElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
		for(ExecutableElement e : methodElements) {
			Shared anno = e.getAnnotation(Shared.class);
			if(anno != null) {
				this.generateTypeInit(builder, e);
			}
		}
		List<ExecutableElement> constructorElements = ElementFilter.constructorsIn(typeElement.getEnclosedElements());
		for(ExecutableElement e : constructorElements) {
			Shared anno = e.getAnnotation(Shared.class);
			if(anno != null) {
				this.generateTypeInit(builder, e);
			}
		}
		builder.writeToFile(this.processingEnv.getFiler());
	}

	/**
	 * 
	 * @param builder
	 * @param element
	 * - - ExecutableElement having Shared annotation
	 */
	private void generateTypeInit(TypeInitBuilder builder, ExecutableElement element) {
		String methodName = element.getSimpleName().toString();
		@SuppressWarnings("unchecked")
		List<VariableElement> varElements = (List<VariableElement>) element.getParameters();
		int size = varElements.size();
		String[] paramTypeNames = new String[size];
		for(int i = 0; i < size; i++) {
			paramTypeNames[i] = this.toTypeName(varElements.get(i));
		}
		String returnTypeName = this.toReturnTypeName(element);
		builder.appendLine(returnTypeName, methodName, paramTypeNames);
	}
}

/**
 * contains generated source code.
 * @author skgchxngsxyz-osx
 *
 */
abstract class SourceBuilder {
	protected final List<String> lineList;

	SourceBuilder() {
		this.lineList = new LinkedList<>();
	}

	void appendLine(String line) {
		this.lineList.add(line);
	}
}

/**
 * contains OperatorTable's source code.
 * @author skgchxngsxyz-osx
 *
 */
class OpTableBuilder extends SourceBuilder {
	public OpTableBuilder() {
		this.appendLine("// auto-generated source code, do not fix me.");
		this.appendLine("package dshell.internal.parser;");
		this.appendLine("");
		this.appendLine("public class OperatorTable extends AbstractOperatorTable {");
		this.appendLine("\tpublic OperatorTable(TypePool pool) {");
	}

	void appendLine(String returnTypeName, String opSymbol, String internalName, String[] paramTypeNames) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("\t\t");
		sBuilder.append("this.setOperator(");
		sBuilder.append(returnTypeName);
		sBuilder.append(", " + "\"" + opSymbol + "\"");
		sBuilder.append(", \"dshell/internal/lib/Operator\", ");
		sBuilder.append("\"" + internalName + "\"");
		for(String paramTypeName : paramTypeNames) {
			sBuilder.append(", " + paramTypeName);
		}
		sBuilder.append(");");
		this.appendLine(sBuilder.toString());
	}

	void writeToFile(Filer filer) {
		this.appendLine("\t}");
		this.appendLine("}");
		try {
			JavaFileObject fileObject = filer.createSourceFile("dshell.internal.parser.OperatorTable");
			Writer writer = fileObject.openWriter();
			for(String line : this.lineList) {
				writer.write(line);
				writer.write("\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class StringWrapperBuilder extends SourceBuilder {
	public StringWrapperBuilder() {
		this.appendLine("// auto-generated source code, do not fix me.");
		this.appendLine("package dshell.internal.initializer;");
		this.appendLine("");
		this.appendLine("public class StringInitializer extends ClassWrapper {");
		this.appendLine("\tprotected void set() {");
		this.appendLine("\t\tthis.createOwnerType(\"dshell/lang/StringUtils\");");
	}

	void appendLine(String returnTypeName, String methodName, String[] paramTypeNames) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("\t\t");
		sBuilder.append("this.setMethod(");
		sBuilder.append(returnTypeName);
		sBuilder.append(", " + "\"" + methodName + "\"");
		for(String paramTypeName : paramTypeNames) {
			sBuilder.append(", " + paramTypeName);
		}
		sBuilder.append(");");
		this.appendLine(sBuilder.toString());
	}

	void writeToFile(Filer filer) {
		this.appendLine("\t}");
		this.appendLine("}");
		try {
			JavaFileObject fileObject = filer.createSourceFile("dshell.internal.initializer.StringInitializer");
			Writer writer = fileObject.openWriter();
			for(String line : this.lineList) {
				writer.write(line);
				writer.write("\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class TypeInitBuilder extends SourceBuilder {
	private final String className;

	public TypeInitBuilder(String className) {
		this.className = className + "Initializer";

		this.appendLine("// auto-generated source code, do not fix me.");
		this.appendLine("package dshell.internal.initializer;");
		this.appendLine("");
		this.appendLine("public class " + this.className + " extends TypeInitializer {");
		this.appendLine("\tprotected void set() {");
	}

	void appendLine(String returnTypeName, String methodName, String[] paramTypeNames) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("\t\t");
		sBuilder.append("this.setMethod(");
		sBuilder.append(returnTypeName);
		sBuilder.append(", " + "\"" + methodName + "\"");
		for(String paramTypeName : paramTypeNames) {
			sBuilder.append(", " + paramTypeName);
		}
		sBuilder.append(");");
		this.appendLine(sBuilder.toString());
	}

	void writeToFile(Filer filer) {
		this.appendLine("\t}");
		this.appendLine("}");
		try {
			JavaFileObject fileObject = filer.createSourceFile("dshell.internal.initializer." + this.className);
			Writer writer = fileObject.openWriter();
			for(String line : this.lineList) {
				writer.write(line);
				writer.write("\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
