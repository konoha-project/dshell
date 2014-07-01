package dshell.annotation;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
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
	"dshell.annotation.SharedClass", "dshell.annotation.TypeAlias", "dshell.annotation.PrimitiveArray"})
public class AnnotationProcessor extends AbstractProcessor {
	private final static String A_SHARED_CLASS = "dshell.annotation.SharedClass";
	private final static String A_OP_HOLDER = "dshell.annotation.OpHolder";

	private final static boolean debugMode = false;

	private boolean isInitialazed = false;

	private void initialize() {
		if(this.isInitialazed) {
			return;
		}
		this.isInitialazed = true;
	}

	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		this.debugPrint("call processor");
		this.initialize();
		TypeInitBuilder initBuilder = new TypeInitBuilder();
		for(TypeElement annotation : annotations) {
			if(this.matchAnnotation(annotation, A_OP_HOLDER)) {
				Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
				if(elements.size() != 1) {
					this.reportErrorAndExit("found duplicated operator table", annotation);
				}
				for(Element element : elements) {
					this.generateOpTable(element);
				}
			} else if(this.matchAnnotation(annotation, A_SHARED_CLASS)) {
				Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
				for(Element element : elements) {
					this.generateInitMethod(initBuilder, element);
				}
			}
		}
		initBuilder.writeToFile(this.processingEnv.getFiler());
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
		if(debugMode) {
			this.processingEnv.getMessager().printMessage(Kind.NOTE, "DEBUG: " + message);
		}
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
			paramTypeNames[i] = builder.toTypeName(varElements.get(i));
		}
		String returnTypeName = builder.toReturnTypeName(element);
		builder.appendLine(returnTypeName, op, methodName, paramTypeNames);
	}

	private void generateInitMethod(TypeInitBuilder initBuilder, Element element) {
		TypeElement typeElement = (TypeElement) element;
		TypeBuilder builder = this.createBuilder(typeElement);

		List<ExecutableElement> methodElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
		for(ExecutableElement e : methodElements) {
			builder.generateMethodElement(e);
		}
		List<ExecutableElement> constructorElements = ElementFilter.constructorsIn(typeElement.getEnclosedElements());
		for(ExecutableElement e : constructorElements) {
			builder.generateConstructorElement(e);
		}
		List<VariableElement> fieldElements = ElementFilter.fieldsIn(typeElement.getEnclosedElements());
		for(VariableElement e : fieldElements) {
			builder.generateFieldElement(e);
		}
		initBuilder.append(builder.finalizeBuilder());
	}

	private TypeBuilder createBuilder(TypeElement typeElement) {	//TODO: final
		String superTypeName = typeElement.getAnnotation(SharedClass.class).value();
		superTypeName = superTypeName.equals("") ? null : superTypeName;
		String className = typeElement.getSimpleName().toString();
		String internalName = typeElement.getQualifiedName().toString().replace('.', '/');

		if(typeElement.getAnnotation(WrapperClass.class) != null) {
			return new StringWrapperBuilder(className, internalName, superTypeName);
		}
		if(typeElement.getAnnotation(PrimitiveArray.class) != null) {
			return new PrimitiveArrayBuilder(className, internalName, superTypeName);
		}
		if(typeElement.getAnnotation(GenericClass.class) != null) {
			GenericClass anno = typeElement.getAnnotation(GenericClass.class);
			StringBuilder sBuilder = new StringBuilder();
			String[] values = anno.values();
			for(int i = 0; i < values.length; i++) {
				if(i > 0) {
					sBuilder.append(" ");
				}
				sBuilder.append(values[i]);
			}
			return new GenericTypeBuilder(className, internalName, superTypeName, sBuilder.toString());
		}
		return new TypeBuilder(className, internalName, superTypeName, true);
	}


	/**
	 * contains OperatorTable's source code.
	 * @author skgchxngsxyz-osx
	 *
	 */
	class OpTableBuilder {
		private final Map<String, String> typeMap = new HashMap<>();

		private final List<String> lineList;
		
		public OpTableBuilder() {
			this.typeMap.put("long", "pool.intType");
			this.typeMap.put("double", "pool.floatType");
			this.typeMap.put("boolean", "pool.booleanType");
			this.typeMap.put("java.lang.String", "pool.stringType");
			this.typeMap.put("String", "pool.stringType");
			this.typeMap.put("java.lang.Object", "pool.objectType");
			this.typeMap.put("Object", "pool.objectType");
			this.typeMap.put("void", "TypePool.voidType");

			this.lineList = new LinkedList<>();
			this.lineList.add("// auto-generated source code, do not fix me.");
			this.lineList.add("package dshell.internal.parser;");
			this.lineList.add("");
			this.lineList.add("import dshell.internal.type.TypePool;");
			this.lineList.add("public class OperatorTable extends AbstractOperatorTable {");
			this.lineList.add("\tpublic OperatorTable(TypePool pool) {");
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
			this.lineList.add(sBuilder.toString());
		}

		void writeToFile(Filer filer) {
			this.lineList.add("\t}");
			this.lineList.add("}");
			try {
				JavaFileObject fileObject = filer.createSourceFile("dshell.internal.parser.OperatorTable");
				Writer writer = fileObject.openWriter();
				for(String line : this.lineList) {
					writer.write(line);
					writer.write("\n");
				}
				writer.close();
			} catch(FilerException e) {
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		String toTypeName(VariableElement variableElement) {
			String name = variableElement.asType().toString();
			String typeName = typeMap.get(name);
			if(typeName == null) {
				reportErrorAndExit("unsupported type: " + name, variableElement);
				return null;
			}
			return typeName;
		}

		String toReturnTypeName(ExecutableElement element) {
			String name = element.getReturnType().toString();
			String typeName = this.typeMap.get(name);
			if(typeName == null) {
				reportErrorAndExit("unsupported  type: " + name, element);
				return null;
			}
			return typeName;
		}
	}

	class TypeInitBuilder {
		private final List<MethodBuilder> builders;

		TypeInitBuilder() {
			this.builders = new ArrayList<>();
		}

		void append(MethodBuilder builder) {
			this.builders.add(builder);
		}

		void writeToFile(Filer filer) {
			try {
				JavaFileObject fileObject = filer.createSourceFile("dshell.internal.type.TypeInitializer");
				Writer writer = fileObject.openWriter();
				writer.write("package dshell.internal.type;\n");
				writer.write("\n");
				writer.write("class TypeInitializer {\n");
				for(MethodBuilder builder : this.builders) {
					for(String line : builder.lineList) {
						writer.write("\t");
						writer.write(line);
						writer.write("\n");
					}
					writer.write("\n");
				}
				writer.write("}\n");
				writer.close();
			} catch(FilerException e) {
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	abstract class MethodBuilder {
		protected List<String> lineList;

		protected MethodBuilder(String methodName) {
			this.lineList = new LinkedList<>();
			this.lineList.add("public static DSType init_" + methodName + "(TypePool pool) {");
		}

		protected void appendLine(String line) {
			this.lineList.add("\t" + line);
		}

		protected void endLine() {
			this.lineList.add("}");
			this.lineList = Collections.unmodifiableList(this.lineList);
		}

		protected String quote(String str) {
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append('"');
			sBuilder.append(str);
			sBuilder.append('"');
			return sBuilder.toString();
		}

		abstract MethodBuilder finalizeBuilder();

		protected String toTypeName(String name, TypeAlias anno) {
			if(anno != null) {
				return anno.value();
			}
			switch(name) {
			case "long": return "int";
			case "double": return "float";
			case "boolean": return "boolean";
			default:
				int index = name.lastIndexOf('.');
				return name.substring(index + 1);
			}
		}

		public String toTypeName(VariableElement variableElement) {
			String name = variableElement.asType().toString();
			TypeAlias anno = variableElement.getAnnotation(TypeAlias.class);
			String typeName = this.toTypeName(name, anno);
			if(typeName == null) {
				reportErrorAndExit("unsupported type: " + name, variableElement);
				return null;
			}
			return typeName;
		}
		
		public String toReturnTypeName(ExecutableElement element) {
			String name = element.getReturnType().toString();
			TypeAlias anno = element.getAnnotation(TypeAlias.class);
			String typeName = this.toTypeName(name, anno);
			if(typeName == null) {
				reportErrorAndExit("unsupported  type: " + name, element);
				return null;
			}
			return typeName;
		}
	}

	class TypeBuilder extends MethodBuilder {
		private final int typeKind;
		private final String className;
		private final String internalName;
		private final String superTypeName;
		private final boolean allowExtends;
		private final String optionalArg;

		private final List<String> constructorElements;
		private final List<String> fieldElements;
		private final List<String> methodElements;

		public TypeBuilder(String className, String internalName, String superTypeName, boolean allowExtends) {
			this(0, className, internalName, superTypeName, allowExtends, null);
		}

		protected TypeBuilder(int typeKind, String className, String internalName, String superTypeName, boolean allowExtends, String optionalArg) {
			super(className);
			this.typeKind = typeKind;
			this.className = className;
			this.internalName = internalName;
			this.superTypeName = superTypeName;
			this.allowExtends = allowExtends;
			this.constructorElements = new ArrayList<>();
			this.fieldElements = new ArrayList<>();
			this.methodElements = new ArrayList<>();
			this.optionalArg = optionalArg;
		}

		void generateMethodElement(ExecutableElement element) {
			if(element.getAnnotation(Shared.class) == null) {
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
			this.appendMethod(methodName, returnTypeName, paramTypeNames);
		}

		void generateConstructorElement(ExecutableElement element) {
			if(element.getAnnotation(Shared.class) == null) {
				return;
			}
			@SuppressWarnings("unchecked")
			List<VariableElement> varElements = (List<VariableElement>) element.getParameters();
			int size = varElements.size();
			String[] paramTypeNames = new String[size];
			for(int i = 0; i < size; i++) {
				paramTypeNames[i] = this.toTypeName(varElements.get(i));
			}
			this.appendConstructor(paramTypeNames);
		}

		void generateFieldElement(VariableElement element) {
			if(element.getAnnotation(Shared.class) == null) {
				return;
			}
			String fieldName = element.getSimpleName().toString();
			String fieldTypeName = this.toTypeName(element);
			this.appendField(fieldName, fieldTypeName);
		}

		protected void appendConstructor(String[] paramTypeNames) {
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append("{");
			int size = paramTypeNames.length;
			for(int i = 0; i < size; i++) {
				if(i > 0) {
					sBuilder.append(", ");
				}
				sBuilder.append(this.quote(paramTypeNames[i]));
			}
			sBuilder.append("},");
			this.constructorElements.add(sBuilder.toString());
		}

		protected void appendField(String fieldName, String fieldTypeName) {
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append("{");
			sBuilder.append(this.quote(fieldName));
			sBuilder.append(", ");
			sBuilder.append(this.quote(fieldTypeName));
			sBuilder.append("},");
			this.fieldElements.add(sBuilder.toString());
		}

		protected void appendMethod(String methodName, String returnTypeName, String[] paramTypeNames) {
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append("{");
			sBuilder.append(this.quote(methodName));
			sBuilder.append(", ");
			sBuilder.append(this.quote(returnTypeName));
			for(String paramTypeName : paramTypeNames) {
				sBuilder.append(", ");
				sBuilder.append(this.quote(paramTypeName));
			}
			sBuilder.append("},");
			this.methodElements.add(sBuilder.toString());
		}

		@Override
		MethodBuilder finalizeBuilder() {
			// create constructor elements
			this.appendElements("ce", this.constructorElements);
			// create field elements
			this.appendElements("fe", this.fieldElements);
			// create method
			this.appendElements("me", this.methodElements);

			if(this.optionalArg == null) {
				this.appendLine("String op = null;");
			} else {
				this.appendLine("String op = " + this.quote(this.optionalArg) + ";");
			}

			if(this.superTypeName == null) {
				this.appendLine("DSType superType = pool.objectType;");
			} else {
				this.appendLine("DSType superType = pool.getType(" + this.quote(this.superTypeName) + ");");
			}
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append("return BuiltinClassType.createType(" + this.typeKind + ", pool, ");
			sBuilder.append(this.quote(this.className));
			sBuilder.append(", ");
			sBuilder.append(this.quote(this.internalName));
			sBuilder.append(", superType, ");
			sBuilder.append(this.allowExtends);
			sBuilder.append(", ce, fe, me, op);");
			this.appendLine(sBuilder.toString());
			this.endLine();
			return this;
		}

		private void appendElements(String name, List<String> elements) {
			if(elements.isEmpty()) {
				this.appendLine("String[][] " + name + " = null;\n");
				return;
			}
			this.appendLine("String[][] " + name + " = {");
			for(String element : elements) {
				this.appendLine("\t\t" + element);
			}
			this.appendLine("};\n");
		}
	}

	class StringWrapperBuilder extends TypeBuilder {
		public StringWrapperBuilder(String className, String internalName, String superTypeName) {
			super(1, className, internalName, superTypeName, false, null);
		}

		@Override
		void generateMethodElement(ExecutableElement element) {
			if(element.getAnnotation(Wrapper.class) == null) {
				return;
			}
			if(element.getAnnotation(Shared.class) == null) {
				return;
			}
			String methodName = element.getSimpleName().toString();
			@SuppressWarnings("unchecked")
			List<VariableElement> varElements = (List<VariableElement>) element.getParameters();
			int size = varElements.size() - 1;
			String[] paramTypeNames = new String[size];
			for(int i = 0; i < size; i++) {
				paramTypeNames[i] = this.toTypeName(varElements.get(i + 1));
			}
			String returnTypeName = this.toReturnTypeName(element);
			this.appendMethod(methodName, returnTypeName, paramTypeNames);
		}
	}

	class PrimitiveArrayBuilder extends TypeBuilder {
		public PrimitiveArrayBuilder(String className, String internalName, String superTypeName) {
			super(2, className, internalName, superTypeName, false, null);
		}
	}

	class GenericTypeBuilder extends TypeBuilder {
		public GenericTypeBuilder(String className, String internalName, String superTypeName, String optionalArg) {
			super(3, className, internalName, superTypeName, false, optionalArg);
		}
	}
}

