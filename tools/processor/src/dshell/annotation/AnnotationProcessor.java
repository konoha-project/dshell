package dshell.annotation;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
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
import javax.tools.JavaFileObject;
import javax.tools.Diagnostic.Kind;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({"dshell.annotation.Shared", "dshell.annotation.OpType", "dshell.annotation.Wrapper"})
public class AnnotationProcessor extends AbstractProcessor {
	private final static String A_SHARED = "dshell.annotation.Shared";
	private final static String A_OPTYPE = "dshell.annotation.OpType";
	private final static String A_WRAPPER = "dshell.annotation.Wrapper";
	

	private OpTableBuilder opTableBuilder;
	private StringWrapperBuilder strBuilder;
	private Map<String, String> typeMap;
	private boolean isInitialazed = false;

	private void initialize() {
		this.opTableBuilder = new OpTableBuilder();
		this.strBuilder = new StringWrapperBuilder();
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
			if(!this.matchAnnotation(annotation, A_SHARED)) {
				continue;
			}
			for(Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
				Annotation anno = element.getAnnotation(OpType.class);
				if(anno != null) {
					this.generateOperator(element, anno);
					continue;
				}
				anno = element.getAnnotation(Wrapper.class);
				if(anno != null) {
					this.generateWrapper(element);
					continue;
				}
			}
		}
		// write to file
		this.opTableBuilder.writeToFile(this.processingEnv.getFiler());
		this.strBuilder.writeToFile(this.processingEnv.getFiler());
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

	private void generateOperator(Element element, Annotation anno) {
		if(!(anno instanceof OpType)) {
			this.reportErrorAndExit("have not @OpType", element);
		}
		String op = ((OpType) anno).value().getOpSymbol();
		if(op.equals("")) {
			this.reportWarning("value is undefined", element);
			return;
		}
		String methodName = element.getSimpleName().toString();
		@SuppressWarnings("unchecked")
		List<VariableElement> varElements = (List<VariableElement>) ((ExecutableElement)element).getParameters();
		int size = varElements.size();
		String[] paramTypeNames = new String[size];
		for(int i = 0; i < size; i++) {
			paramTypeNames[i] = this.toTypeName(varElements.get(i));
		}
		String returnTypeName = this.toReturnTypeName((ExecutableElement) element);
		this.opTableBuilder.appendLine(returnTypeName, op, methodName, paramTypeNames);
	}

	private void generateWrapper(Element element) {
		String methodName = element.getSimpleName().toString();
		@SuppressWarnings("unchecked")
		List<VariableElement> varElements = (List<VariableElement>) ((ExecutableElement)element).getParameters();
		String recvTypeName = this.toTypeName(varElements.get(0));
		int size = varElements.size() - 1;
		String[] paramTypeNames = new String[size];
		for(int i = 0; i < size; i++) {
			paramTypeNames[i] = this.toTypeName(varElements.get(i + 1));
		}
		String returnTypeName = this.toReturnTypeName((ExecutableElement) element);
		this.strBuilder.appendLine(recvTypeName, returnTypeName, methodName, paramTypeNames);
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
		this.appendLine("package dshell.internal.parser;");
		this.appendLine("");
		this.appendLine("import dshell.internal.parser.TypePool.ClassType;");
		this.appendLine("");
		this.appendLine("public class StringWrapper extends ClassWrapper {");
		this.appendLine("\tpublic void set(ClassType classType, TypePool pool) {");
		this.appendLine("\t\tthis.classType = classType;");
		this.appendLine("\t\tthis.pool = pool;");
		this.appendLine("\t\tthis.createOwnerType(\"dshell/lang/StringUtils\");");
	}

	void appendLine(String recvTypeName, String returnTypeName, String methodName, String[] paramTypeNames) {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("\t\t");
		sBuilder.append("this.setStaticMethod(");
		sBuilder.append(recvTypeName + ", ");
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
			JavaFileObject fileObject = filer.createSourceFile("dshell.internal.parser.StringWrapper");
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
