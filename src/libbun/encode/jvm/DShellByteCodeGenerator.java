package libbun.encode.jvm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.EmptyNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.binary.BunInstanceOfNode;
import libbun.ast.decl.BunClassNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.ast.decl.TopLevelNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.statement.BunReturnNode;
import libbun.ast.statement.BunThrowNode;
import libbun.ast.sugar.BunContinueNode;
import libbun.encode.jvm.JavaMethodTable;
import libbun.encode.jvm.JavaTypeTable;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import dshell.ast.CommandNode;
import dshell.ast.DShellCatchNode;
import dshell.ast.DShellForNode;
import dshell.ast.DShellTryNode;
import dshell.ast.DShellWrapperNode;
import dshell.ast.InternalFuncCallNode;
import dshell.ast.MatchRegexNode;
import dshell.ast.sugar.DShellExportEnvNode;
import dshell.ast.sugar.DShellImportEnvNode;
import dshell.exception.DShellException;
import dshell.exception.Errno;
import dshell.exception.MultipleException;
import dshell.exception.NativeException;
import dshell.lang.DShellTypeChecker;
import dshell.lang.DShellVisitor;
import dshell.lib.CommandArg;
import dshell.lib.CommandArg.SubstitutedArg;
import dshell.lib.GlobalVariableTable;
import dshell.lib.RuntimeContext;
import dshell.lib.StreamUtils;
import dshell.lib.Task;
import dshell.lib.TaskBuilder;
import dshell.lib.Utils;
import libbun.parser.classic.BTokenContext;
import libbun.parser.classic.LibBunLogger;
import libbun.parser.classic.ParserSource;
import libbun.type.BFormFunc;
import libbun.type.BFuncType;
import libbun.type.BGenericType;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.util.BArray;
import libbun.util.LibBunSystem;

public class DShellByteCodeGenerator extends AsmJavaGenerator implements DShellVisitor {
	private BunFunctionNode untypedMainNode = null;
	protected final LinkedList<TopLevelStatementInfo> topLevelStatementList;

	private Method execCommandVoid;
	private Method execCommandBool;
	private Method execCommandInt;
	private Method execCommandString;
	private Method execCommandStringArray;
	private Method execCommandTask;
	private Method execCommandTaskArray;

	private Method wrapException;

	public DShellByteCodeGenerator() {
		super();
		this.topLevelStatementList = new LinkedList<TopLevelStatementInfo>();
		this.loadJavaClass(Task.class);
		this.loadJavaClass(dshell.exception.Exception.class);
		this.loadJavaClass(DShellException.class);
		this.loadJavaClass(MultipleException.class);
		this.loadJavaClass(Errno.UnimplementedErrnoException.class);
		this.loadJavaClass(DShellException.NullException.class);
		this.loadJavaClass(NativeException.class);
		this.loadJavaClassList(Errno.getExceptionClassList());
		this.loadJavaClass(CommandArg.class);
		this.loadJavaClass(SubstitutedArg.class);
		this.loadJavaClass(StreamUtils.InputStream.class);
		this.loadJavaClass(StreamUtils.OutputStream.class);

		try {
			this.execCommandVoid = TaskBuilder.class.getMethod("execCommandVoid", CommandArg[][].class);
			this.execCommandBool = TaskBuilder.class.getMethod("execCommandBool", CommandArg[][].class);
			this.execCommandInt = TaskBuilder.class.getMethod("execCommandInt", CommandArg[][].class);
			this.execCommandString = TaskBuilder.class.getMethod("execCommandString", CommandArg[][].class);
			this.execCommandStringArray = TaskBuilder.class.getMethod("execCommandStringArray", CommandArg[][].class);
			this.execCommandTask = TaskBuilder.class.getMethod("execCommandTask", CommandArg[][].class);
			this.execCommandTaskArray = TaskBuilder.class.getMethod("execCommandTaskArray", CommandArg[][].class);

			this.wrapException = NativeException.class.getMethod("wrapException", Throwable.class);
		}
		catch(Throwable e) {
			e.printStackTrace();
			Utils.fatal(1, "method loading failed");
		}
		JavaMethodTable.Import(BType.StringType, "=~", BType.StringType, Utils.class, "matchRegex");
		JavaMethodTable.Import(BType.StringType, "!~", BType.StringType, Utils.class, "unmatchRegex");

		// load static method
		this.loadJavaStaticMethod(Utils.class, "getEnv", String.class);
		this.loadJavaStaticMethod(Utils.class, "setEnv", String.class, String.class);
		this.loadJavaStaticMethod(CommandArg.class, "createCommandArg", String.class);
		this.loadJavaStaticMethod(CommandArg.class, "createSubstitutedArg", String.class);
		this.loadJavaStaticMethod(Utils.class, "assertDShell", boolean.class);
		this.loadJavaStaticMethod(JavaCommonApi.class, "_", "ObjectToString", Object.class);
		this.loadJavaStaticMethod(Utils.class, "_", "stringToLong", String.class);
		this.loadJavaStaticMethod(Utils.class, "_", "stringToDouble", String.class);
	}

	@Override
	public void visitCommandNode(CommandNode node) {
		this.AsmBuilder.SetLineNumber(node);
		ArrayList<CommandNode> nodeList = new ArrayList<CommandNode>();
		CommandNode commandNode = node;
		while(commandNode != null) {
			nodeList.add(commandNode);
			commandNode = (CommandNode) commandNode.getPipedNextNode();
		}
		// new String[n][]
		int size = nodeList.size();
		this.AsmBuilder.visitLdcInsn(size);
		this.AsmBuilder.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(CommandArg[].class));
		for(int i = 0; i < size; i++) {
			// new String[m];
			CommandNode currentNode = nodeList.get(i);
			int listSize = currentNode.getArgSize();
			this.AsmBuilder.visitInsn(Opcodes.DUP);
			this.AsmBuilder.visitLdcInsn(i);
			this.AsmBuilder.visitLdcInsn(listSize);
			this.AsmBuilder.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(CommandArg.class));
			for(int j = 0; j < listSize; j++ ) {
				this.AsmBuilder.visitInsn(Opcodes.DUP);
				this.AsmBuilder.visitLdcInsn(j);
				currentNode.getArgAt(j).Accept(this);
				this.AsmBuilder.visitInsn(Opcodes.AASTORE);
			}
			this.AsmBuilder.visitInsn(Opcodes.AASTORE);
		}

		if(node.Type.IsBooleanType()) {
			this.invokeStaticMethod(node, this.execCommandBool);
		}
		else if(node.Type.IsIntType()) {
			this.invokeStaticMethod(node, this.execCommandInt);
		}
		else if(node.Type.IsStringType()) {
			this.invokeStaticMethod(node, this.execCommandString);
		}
		else if(node.Type.equals(BTypePool._GetGenericType1(BGenericType._ArrayType, BType.StringType))) {
			this.invokeStaticMethod(node, this.execCommandStringArray);
		}
		else if(node.Type.equals(JavaTypeTable.GetBunType(Task.class))) {
			this.invokeStaticMethod(node, this.execCommandTask);
		}
		else if(node.Type.equals(BTypePool._GetGenericType1(BGenericType._ArrayType, JavaTypeTable.GetBunType(Task.class)))) {
			this.invokeStaticMethod(node, this.execCommandTaskArray);
		}
		else {
			this.invokeStaticMethod(node, this.execCommandVoid);
		}
	}

	@Override
	public void visitTryNode(DShellTryNode node) {
		AsmTryCatchLabel label = new AsmTryCatchLabel();
		this.TryCatchLabel.push(label); // push
		// try block
		this.AsmBuilder.visitLabel(label.BeginTryLabel);
		node.tryBlockNode().Accept(this);
		this.AsmBuilder.visitLabel(label.EndTryLabel);
		this.AsmBuilder.visitJumpInsn(Opcodes.GOTO, label.FinallyLabel);
		// catch block
		int size = node.GetListSize();
		for(int i = 0; i < size; i++) {
			node.GetListAt(i).Accept(this);
		}
		// finally block
		this.AsmBuilder.visitLabel(label.FinallyLabel);
		if(node.hasFinallyBlockNode()) {
			node.finallyBlockNode().Accept(this);
		}
		this.TryCatchLabel.pop();
	}

	@Override
	public void visitCatchNode(DShellCatchNode node) {
		Label catchLabel = new Label();
		AsmTryCatchLabel Label = this.TryCatchLabel.peek();

		// prepare
		String throwType = this.resolveExceptionType(node);
		this.AsmBuilder.visitTryCatchBlock(Label.BeginTryLabel, Label.EndTryLabel, catchLabel, throwType);

		// catch block
		this.AsmBuilder.AddLocal(this.GetJavaClass(node.exceptionType()), node.exceptionName());
		this.AsmBuilder.visitLabel(catchLabel);
		this.invokeExceptionWrapper(node);
		node.blockNode().Accept(this);
		this.AsmBuilder.visitJumpInsn(Opcodes.GOTO, Label.FinallyLabel);

		this.AsmBuilder.RemoveLocal(this.GetJavaClass(node.exceptionType()), node.exceptionName());
	}

	private String resolveExceptionType(DShellCatchNode node) {
		if(!node.hasTypeInfo()) {
			return Type.getType(Throwable.class).getInternalName();
		}
		return this.AsmType(node.exceptionType()).getInternalName();
	}

	private void invokeExceptionWrapper(DShellCatchNode node) {
		if(!node.hasTypeInfo()) {
			this.invokeStaticMethod(null, this.wrapException);
		}
		this.AsmBuilder.StoreLocal(node.exceptionName());
	}

	@Override public void VisitThrowNode(BunThrowNode node) {
		node.ExprNode().Accept(this);
		this.AsmBuilder.visitInsn(Opcodes.ATHROW);
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode node) {
		if(!(node.LeftNode().Type instanceof BGenericType) && !(node.LeftNode().Type instanceof BFuncType)) {
			this.VisitNativeInstanceOfNode(node);
			return;
		}
		node.LeftNode().Accept(this);
		this.AsmBuilder.Pop(node.LeftNode().Type);
		this.AsmBuilder.PushLong(node.LeftNode().Type.TypeId);
		this.AsmBuilder.PushLong(node.TargetType().TypeId);
		Method method = JavaMethodTable.GetBinaryStaticMethod(BType.IntType, "==", BType.IntType);
		this.invokeStaticMethod(null, method);
	}

	private void VisitNativeInstanceOfNode(BunInstanceOfNode node) {
		if(!node.TargetType().Equals(JavaTypeTable.GetBunType(this.GetJavaClass(node.TargetType())))) {
			node.LeftNode().Accept(this);
			this.AsmBuilder.Pop(node.LeftNode().Type);
			this.AsmBuilder.PushBoolean(false);
			return;
		}
		Class<?> javaClass = this.GetJavaClass(node.TargetType());
		if(node.TargetType().IsIntType()) {
			javaClass = Long.class;
		}
		else if(node.TargetType().IsFloatType()) {
			javaClass = Double.class;
		}
		else if(node.TargetType().IsBooleanType()) {
			javaClass = Boolean.class;
		}
		this.invokeBoxingMethod(node.LeftNode());
		this.AsmBuilder.visitTypeInsn(Opcodes.INSTANCEOF, javaClass);
	}

	private void invokeBoxingMethod(BNode targetNode) {
		Class<?> targetClass = Object.class;
		if(targetNode.Type.IsIntType()) {
			targetClass = Long.class;
		}
		else if(targetNode.Type.IsFloatType()) {
			targetClass = Double.class;
		}
		else if(targetNode.Type.IsBooleanType()) {
			targetClass = Boolean.class;
		}
		Class<?> sourceClass = this.GetJavaClass(targetNode.Type);
		Method sMethod = JavaMethodTable.GetCastMethod(targetClass, sourceClass);
		targetNode.Accept(this);
		if(!targetClass.equals(Object.class)) {
			this.invokeStaticMethod(null, sMethod);
		}
	}

	@Override public void VisitErrorNode(ErrorNode node) {
		assert node.SourceToken != null;
		LibBunLogger._LogError(node.SourceToken, node.ErrorMessage);
		throw new ErrorNodeFoundException();
	}

	@Override public void VisitSyntaxSugarNode(SyntaxSugarNode node) {
		if(node instanceof BunContinueNode) {
			this.visitContinueNode((BunContinueNode) node);
		}
		else {
			super.VisitSyntaxSugarNode(node);
		}
	}

	@Override
	public void visitContinueNode(BunContinueNode node) {
		Label l = this.AsmBuilder.ContinueLabelStack.peek();
		this.AsmBuilder.visitJumpInsn(Opcodes.GOTO, l);
	}

	@Override
	public void visitForNode(DShellForNode node) {
		Label headLabel = new Label();
		Label continueLabel = new Label();
		Label breakLabel = new Label();
		this.AsmBuilder.BreakLabelStack.push(breakLabel);
		this.AsmBuilder.ContinueLabelStack.push(continueLabel);

		if(node.hasDeclNode()) {
			this.VisitVarDeclNode(node.toVarDeclNode());
		}
		this.AsmBuilder.visitLabel(headLabel);
		node.condNode().Accept(this);
		this.AsmBuilder.visitJumpInsn(Opcodes.IFEQ, breakLabel);
		node.blockNode().Accept(this);
		this.AsmBuilder.visitLabel(continueLabel);
		if(node.hasNextNode()) {
			node.nextNode().Accept(this);
		}
		this.AsmBuilder.visitJumpInsn(Opcodes.GOTO, headLabel);
		this.AsmBuilder.visitLabel(breakLabel);
		if(node.hasDeclNode()) {
			this.VisitVarDeclNode2(node.toVarDeclNode());
		}

		this.AsmBuilder.BreakLabelStack.pop();
		this.AsmBuilder.ContinueLabelStack.pop();
	}

	@Override
	protected void VisitVarDeclNode(BunLetVarNode node) {
		String varName = node.GetGivenName();
		if(this.AsmBuilder.FindLocalVariable(varName) != null) {
			this.VisitErrorNode(new ErrorNode(node, varName + " is already defined"));
			return;
		}
		Class<?> declClass = this.GetJavaClass(node.DeclType());
		this.AsmBuilder.AddLocal(declClass, varName);
		this.AsmBuilder.PushNode(declClass, node.InitValueNode());
		this.AsmBuilder.StoreLocal(varName);
	}

	@Override
	public void visitWrapperNode(DShellWrapperNode node) {
		node.getTargetNode().Accept(this);
	}

	private int getTypeId(BType type) {
		if(type.IsIntType()) {
			return GlobalVariableTable.LONG_TYPE;
		}
		else if(type.IsFloatType()) {
			return GlobalVariableTable.DOUBLE_TYPE;
		}
		else if(type.IsBooleanType()) {
			return GlobalVariableTable.BOOLEAN_TYPE;
		}
		else {
			return GlobalVariableTable.OBJECT_TYPE;
		}
	}

	private void setVariable(int varIndex, int typeId, BNode valueNode) {
		String owner = Type.getInternalName(GlobalVariableTable.class);
		switch(typeId) {
		case GlobalVariableTable.LONG_TYPE:
			this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "longVarTable", Type.getDescriptor(long[].class));
			this.AsmBuilder.PushInt(varIndex);
			this.AsmBuilder.PushNode(long.class, valueNode);
			this.AsmBuilder.visitInsn(Opcodes.LASTORE);
			break;
		case GlobalVariableTable.DOUBLE_TYPE:
			this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "doubleVarTable", Type.getDescriptor(double[].class));
			this.AsmBuilder.PushInt(varIndex);
			this.AsmBuilder.PushNode(double.class, valueNode);
			this.AsmBuilder.visitInsn(Opcodes.DASTORE);
			break;
		case GlobalVariableTable.BOOLEAN_TYPE:
			this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "booleanVarTable", Type.getDescriptor(boolean[].class));
			this.AsmBuilder.PushInt(varIndex);
			this.AsmBuilder.PushNode(boolean.class, valueNode);
			this.AsmBuilder.visitInsn(Opcodes.BASTORE);
			break;
		case GlobalVariableTable.OBJECT_TYPE:
			this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "objectVarTable", Type.getDescriptor(Object[].class));
			this.AsmBuilder.PushInt(varIndex);
			this.AsmBuilder.PushNode(null, valueNode);
			this.AsmBuilder.visitInsn(Opcodes.AASTORE);
			break;
		}
	}

	@Override
	public void VisitLetNode(BunLetVarNode node) {
		String varName = node.GetGivenName();
		if(GlobalVariableTable.existEntry(varName)) {
			this.VisitErrorNode(new ErrorNode(node, varName + " is already defined"));
			return;
		}
		int typeId = this.getTypeId(node.DeclType());
		int varIndex = GlobalVariableTable.addEntry(varName, typeId, node.IsReadOnly());
		try {
			this.setVariable(varIndex, typeId, node.InitValueNode());
		}
		catch(Throwable t) {
			GlobalVariableTable.removeEntry(varName);
			if(!(t instanceof RuntimeException)) {
				t = new RuntimeException(t);
			}
			throw (RuntimeException)t;
		}
	}

	@Override
	protected void VisitGlobalNameNode(GetNameNode node) {
		BunLetVarNode letNode = node.ResolvedNode;
		String varName = letNode.GetGivenName();
		int typeId = this.getTypeId(letNode.DeclType());
		int varIndex = GlobalVariableTable.getVarIndex(varName, typeId);
		if(varIndex == -1) {
			this.VisitErrorNode(new ErrorNode(node, "undefiend varibale: " + varName));
			return;
		}
		String owner = Type.getInternalName(GlobalVariableTable.class);
		switch(typeId) {
		case GlobalVariableTable.LONG_TYPE:
			this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "longVarTable", Type.getDescriptor(long[].class));
			this.AsmBuilder.PushInt(varIndex);
			this.AsmBuilder.visitInsn(Opcodes.LALOAD);
			break;
		case GlobalVariableTable.DOUBLE_TYPE:
			this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "doubleVarTable", Type.getDescriptor(double[].class));
			this.AsmBuilder.PushInt(varIndex);
			this.AsmBuilder.visitInsn(Opcodes.DALOAD);
			break;
		case GlobalVariableTable.BOOLEAN_TYPE:
			this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "booleanVarTable", Type.getDescriptor(boolean[].class));
			this.AsmBuilder.PushInt(varIndex);
			this.AsmBuilder.visitInsn(Opcodes.BALOAD);
			break;
		case GlobalVariableTable.OBJECT_TYPE:
			this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "objectVarTable", Type.getDescriptor(Object[].class));
			this.AsmBuilder.PushInt(varIndex);
			this.AsmBuilder.visitInsn(Opcodes.AALOAD);
			this.AsmBuilder.visitTypeInsn(Opcodes.CHECKCAST, this.GetJavaClass(node.Type));
			break;
		}
	}

	@Override
	protected void GenerateAssignNode(GetNameNode node, BNode exprNode) {
		if(node.ResolvedNode == null) {
			this.VisitErrorNode(new ErrorNode(node, "undefined symbol: " + node.GivenName));
			return;
		}
		if(node.ResolvedNode.IsReadOnly() && !(node.ResolvedNode.ParentNode instanceof BunFunctionNode)) {
			this.VisitErrorNode(new ErrorNode(node, "read only variable: " + node.GivenName));
			return;
		}
		if(node.ResolvedNode.GetDefiningFunctionNode() == null) {
			int typeId = this.getTypeId(exprNode.Type);
			int varIndex = GlobalVariableTable.getVarIndex(node.ResolvedNode.GetGivenName(), typeId);
			this.setVariable(varIndex, typeId, exprNode);
			return;
		}
		String name = node.GetUniqueName(this);
		this.AsmBuilder.PushNode(this.AsmBuilder.GetLocalType(name), exprNode);
		this.AsmBuilder.StoreLocal(name);
	}

	@Override
	public void visitMatchRegexNode(MatchRegexNode node) {
		this.VisitBinaryNode(node);
	}

	@Override
	public void visitInternalFuncCallNode(InternalFuncCallNode node) {
		this.invokeStaticMethod(null, node.getMethod());
	}

	// utils for visitor
	protected void invokeStaticMethod(BNode Node, Method method) {
		String owner = Type.getInternalName(method.getDeclaringClass());
		this.AsmBuilder.visitMethodInsn(Opcodes.INVOKESTATIC, owner, method.getName(), Type.getMethodDescriptor(method));
		if(Node != null) {
			this.AsmBuilder.CheckReturnCast(Node, method.getReturnType());
		}
	}

	protected void loadJavaClass(Class<?> classObject) {
		BType type = JavaTypeTable.GetBunType(classObject);
		this.RootGamma.SetTypeName(type, null);
	}

	protected void loadJavaClassList(ArrayList<Class<?>> classObjList) {
		for(Class<?> classObj : classObjList) {
			this.loadJavaClass(classObj);
		}
	}

	protected void loadJavaStaticMethod(Class<?> holderClass, String internalName, Class<?>... paramClasses) {
		this.loadJavaStaticMethod(holderClass, internalName, internalName, paramClasses);
	}

	protected void loadJavaStaticMethod(Class<?> holderClass, String name, String internalName, Class<?>... paramClasses) {
		this.loadJavaStaticMethod(holderClass, name, internalName, null, this.toBTypes(paramClasses));
	}

	protected void loadJavaStaticMethod(Class<?> holderClass, String name, BType returnType, BType... paramTypes) {
		this.loadJavaStaticMethod(holderClass, name, name, returnType, paramTypes);
	}

	protected void loadJavaStaticMethod(Class<?> holderClass, String name, String internalName, BType returnType, BType... paramTypes) {
		String formSymbol = name;
		String holderClassPath = holderClass.getCanonicalName().replaceAll("\\.", "/");
		BArray<BType> typeList = new BArray<BType>(new BType[4]);
		StringBuilder formBuilder = new StringBuilder();
		formBuilder.append(holderClassPath + "." + internalName + "(");
		for(int i = 0; i < paramTypes.length; i++) {
			if(i != 0) {
				formBuilder.append(",");
			}
			formBuilder.append("$[" + i + "]");
			typeList.add(paramTypes[i]);
		}
		formBuilder.append(")");
		if(returnType == null) {
			try {
				returnType = JavaTypeTable.GetBunType(holderClass.getMethod(internalName, this.toClasses(paramTypes)).getReturnType());
			}
			catch(Throwable e) {
				Utils.fatal(1, "load static method faild: " + e.getMessage());
			}
		}
		typeList.add(returnType);
		BFuncType funcType = (BFuncType) BTypePool._GetGenericType(BFuncType._FuncType, typeList, true);
		BFormFunc formFunc = new BFormFunc(formSymbol, funcType, null, formBuilder.toString());
		if(name.equals("_")) {
			this.SetConverterFunc(funcType.GetRecvType(), funcType.GetReturnType(), formFunc);
		}
		else {
			this.SetDefinedFunc(formFunc);
		}
	}

	protected Class<?>[] toClasses(BType[] types) {
		int size = types.length;
		Class<?>[] classes = new Class<?>[size];
		for(int i = 0; i < size; i++) {
			classes[i] = this.GetJavaClass(types[i]);
		}
		return classes;
	}

	protected BType[] toBTypes(Class<?>[] classes) {
		int size = classes.length;
		BType[] types = new BType[size];
		for(int i = 0; i < size; i++) {
			types[i] = JavaTypeTable.GetBunType(classes[i]);
		}
		return types;
	}

	// utils for execution
	protected boolean loadScript(String script, String fileName, int lineNumber, boolean isInteractive) {
		boolean result = true;
		BunBlockNode topBlockNode = new BunBlockNode(null, this.RootGamma);
		ParserSource source = new ParserSource(fileName, lineNumber, script, this.Logger);
		BTokenContext tokenContext = new BTokenContext(this.RootParser, this, source, 0, script.length());
		tokenContext.SkipEmptyStatement();
		while(tokenContext.HasNext()) {
			tokenContext.SetParseFlag(BTokenContext._NotAllowSkipIndent);
			topBlockNode.ClearListToSize(0);
			BNode stmtNode;
			try {
				stmtNode = tokenContext.ParsePattern(topBlockNode, "$Statement$", BTokenContext._Required);
			}
			catch(Throwable e) {
				System.err.println("Parsing Problem");
				e.printStackTrace();
				this.topLevelStatementList.clear();
				return false;
			}
			if(!this.generateStatement(stmtNode, isInteractive)) {
				result = false;
				break;
			}
			tokenContext.SkipEmptyStatement();
			tokenContext.Vacume();
		}
		this.Logger.OutputErrorsToStdErr();
		return result;
	}

	public boolean loadLine(String line, int lineNumber, boolean isInteractive) {
		return this.loadScript(line, "(stdin)", lineNumber, isInteractive);
	}

	public void loadArg(String[] scriptArgs) {
		StringBuilder argvBuilder = new StringBuilder();
		argvBuilder.append("let ARGV = [");
		for(int i = 0; i < scriptArgs.length; i++) {
			if(i != 0) {
				argvBuilder.append(", ");
			}
			argvBuilder.append("\"");
			argvBuilder.append(scriptArgs[i]);
			argvBuilder.append("\"");
		}
		argvBuilder.append("]");
		this.loadScript(argvBuilder.toString(), scriptArgs[0], 0, false);
	}

	public boolean loadFile(String fileName) {
		String script = LibBunSystem._LoadTextFile(fileName);
		if(script == null) {
			System.err.println("file not found: " + fileName);
			System.exit(1);
		}
		return this.loadScript(script, fileName, 1, false);
	}

	public void loadDShellrc() {
		final String fileName = RuntimeContext.getContext().getenv("HOME") + "/.dshellrc";
		String script = LibBunSystem._LoadTextFile(fileName);
		if(script != null && this.loadScript(script, fileName, 0, true)) {
			this.evalAndPrint();
		}
	}

	public void loadVariables(boolean isInteractive) {
		BNode parentNode = new BunBlockNode(null, this.RootGamma);
		ArrayList<BNode> nodeList = new ArrayList<BNode>();
		nodeList.add(this.createVarNode(parentNode, "stdin", StreamUtils.class, "createStdin"));
		nodeList.add(this.createVarNode(parentNode, "stdout", StreamUtils.class, "createStdout"));
		nodeList.add(this.createVarNode(parentNode, "stderr", StreamUtils.class, "createStderr"));
		for(BNode node : nodeList) {
			this.generateStatement(node, isInteractive);
			this.evalAndPrint();
		}
	}

	public BNode createVarNode(BNode parentNode, String varName, Class<?> holderClass, String methodName) {
		BunLetVarNode node = new BunLetVarNode(parentNode, BunLetVarNode._IsReadOnly, null, varName);
		node.SetNode(BunLetVarNode._InitValue, new InternalFuncCallNode(node, holderClass, methodName));
		return node;
	}

	protected void generateByteCode(BNode node) {
		try {
			node.Accept(this);
		}
		catch(ErrorNodeFoundException e) {
			this.topLevelStatementList.clear();
			if(RuntimeContext.getContext().isDebugMode()) {
				e.printStackTrace();
			}
			this.StopVisitor();
		}
		catch(Throwable e) {
			System.err.println("Code Generation Failed");
			e.printStackTrace();
			this.topLevelStatementList.clear();
			this.StopVisitor();
		}
	}

	protected boolean generateStatement(BNode node, boolean IsInteractive) {
		this.EnableVisitor();
		if(node instanceof EmptyNode) {
			return this.IsVisitable();
		}
		if(node instanceof TopLevelNode) {
			((TopLevelNode)node).Perform(this.RootGamma);
			return this.IsVisitable();
		}
		node = this.checkTopLevelSupport(node);
		if(node.IsErrorNode()) {
			this.generateByteCode(node);
		}
		else if(IsInteractive && (node instanceof DShellWrapperNode) && !((DShellWrapperNode)node).isVarTarget()) {
			node = this.TypeChecker.CheckType(node, BType.VoidType);
			this.generateByteCode(node);
		}
		else if(IsInteractive) {
			BunFunctionNode funcNode = ((DShellTypeChecker)this.TypeChecker).visitTopLevelStatementNode(node);
			this.topLevelStatementList.add(new TopLevelStatementInfo(funcNode.GivenName, funcNode.ReturnType()));
			this.generateByteCode(funcNode);
		}
		else {
			if(this.untypedMainNode == null) {
				this.untypedMainNode = new BunFunctionNode(node.ParentNode);
				this.untypedMainNode.GivenName = "main";
				this.untypedMainNode.SourceToken = node.SourceToken;
				this.untypedMainNode.SetNode(BunFunctionNode._Block, new BunBlockNode(this.untypedMainNode, null));
			}
			this.untypedMainNode.BlockNode().Append(node);
		}
		return this.IsVisitable();
	}

	protected boolean evalAndPrintEachNode(TopLevelStatementInfo info) {
		Class<?> funcClass = this.GetDefinedFunctionClass(info.funcName, BType.VoidType, 0);
		try {
			Method method = funcClass.getMethod("f");
			Object value = method.invoke(null);
			if(!info.returnType.IsVoidType()) {
				System.out.println(" (" + info.returnType + ") " + value);
			}
			return true;
		}
		catch(InvocationTargetException e) {
			this.printException(e);
		}
		catch(Throwable e) {
			e.printStackTrace();
			Utils.fatal(1, "invocation problem");
		}
		return false;
	}

	public void evalAndPrint() {
		while(!this.topLevelStatementList.isEmpty()) {
			TopLevelStatementInfo info = this.topLevelStatementList.remove();
			if(!this.evalAndPrintEachNode(info)) {
				this.topLevelStatementList.clear();
			}
		}
	}

	public void invokeMain() {
		if(this.untypedMainNode == null) {
			System.err.println("not found main");
			System.exit(1);
		}
		try {
			BunFunctionNode node = (BunFunctionNode) this.TypeChecker.CheckType(this.untypedMainNode, BType.VarType);
			node.Type = BType.VoidType;
			node.IsExport = true;
			node.Accept(this);
			this.Logger.OutputErrorsToStdErr();
		}
		catch(ErrorNodeFoundException e) {
			this.Logger.OutputErrorsToStdErr();
			if(RuntimeContext.getContext().isDebugMode()) {
				e.printStackTrace();
			}
			System.exit(1);
		}
		catch(Throwable e) {
			e.printStackTrace();
			System.err.println("Code Generation Failed");
			System.exit(1);
		}
		if(this.MainFuncNode != null) {
			JavaStaticFieldNode mainFunc = this.MainFuncNode;
			try {
				Method Method = mainFunc.StaticClass.getMethod("f");
				Method.invoke(null);
				System.exit(0);
			}
			catch(InvocationTargetException e) {
				this.printException(e);
				System.exit(1);
			}
			catch(Throwable e) {
				e.printStackTrace();
				Utils.fatal(1, "invocation problem");
			}
		}
	}

	private BNode checkTopLevelSupport(BNode Node) {
		if(Node instanceof BunVarBlockNode) {
			return new ErrorNode(Node, "only available inside function");
		}
		else if(Node instanceof BunClassNode || Node instanceof BunFunctionNode) {
			return new DShellWrapperNode(Node);
		}
		else if(Node instanceof BunLetVarNode || Node instanceof DShellExportEnvNode || Node instanceof DShellImportEnvNode) {
			return new DShellWrapperNode(Node, true);
		}
		BunReturnNode ReturnNode = this.findReturnNode(Node);
		if(ReturnNode != null) {
			return new ErrorNode(ReturnNode, "only available inside function");
		}
		return Node;
	}

	private BunReturnNode findReturnNode(BNode Node) {
		if(Node == null || Node.IsErrorNode()) {
			return null;
		}
		else if(Node instanceof BunReturnNode) {
			return (BunReturnNode) Node;
		}
		else if(Node instanceof BunBlockNode) {
			BunBlockNode BlockNode = (BunBlockNode) Node;
			int size = BlockNode.GetListSize();
			for(int i = 0; i < size; i++) {
				BunReturnNode ReturnNode = this.findReturnNode(BlockNode.GetListAt(i));
				if(ReturnNode != null) {
					return ReturnNode;
				}
			}
		}
		int size = Node.GetAstSize();
		for(int i = 0; i < size; i++) {
			BunReturnNode ReturnNode = this.findReturnNode(Node.AST[i]);
			if(ReturnNode != null) {
				return ReturnNode;
			}
		}
		return null;
	}

	private void printException(InvocationTargetException e) {
		NativeException.wrapException(e.getCause()).printStackTrace();
	}

	protected void clearCurrentFunction(BNode node) {	//TODO:
		//Class<?> FuncClass = this.GetDefinedFunctionClass(info.funcName, BType.VoidType, 0);
	}

	protected void clearTopLevelStatementList() {	//TODO:
		for(TopLevelStatementInfo info : this.topLevelStatementList) {
			
		}
		this.topLevelStatementList.clear();
	}

	protected Class<?> removeDefinedFuncClass(String funcName, BType recvType, int funcParamSize) {
		//return this.GeneratedClassMap.GetOrNull(this.NameFunctionClass(FuncName, RecvType, FuncParamSize));
		return null;
	}

	private static class ErrorNodeFoundException extends RuntimeException {
		private static final long serialVersionUID = -2465006344250569543L;
	}
}

class TopLevelStatementInfo {
	public final String funcName;
	public final BType returnType;

	public TopLevelStatementInfo(String funcName, BType returnType) {
		this.funcName = funcName;
		this.returnType = returnType;
	}
}

class TopLevelStatementList {
	private LinkedList<TopLevelStatementInfo> topLevelStatementList;
}
