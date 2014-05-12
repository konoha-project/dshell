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

	private Method ExecCommandVoid;
	private Method ExecCommandBool;
	private Method ExecCommandInt;
	private Method ExecCommandString;
	private Method ExecCommandStringArray;
	private Method ExecCommandTask;
	private Method ExecCommandTaskArray;

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
			this.ExecCommandVoid = TaskBuilder.class.getMethod("ExecCommandVoid", CommandArg[][].class);
			this.ExecCommandBool = TaskBuilder.class.getMethod("ExecCommandBool", CommandArg[][].class);
			this.ExecCommandInt = TaskBuilder.class.getMethod("ExecCommandInt", CommandArg[][].class);
			this.ExecCommandString = TaskBuilder.class.getMethod("ExecCommandString", CommandArg[][].class);
			this.ExecCommandStringArray = TaskBuilder.class.getMethod("ExecCommandStringArray", CommandArg[][].class);
			this.ExecCommandTask = TaskBuilder.class.getMethod("ExecCommandTask", CommandArg[][].class);
			this.ExecCommandTaskArray = TaskBuilder.class.getMethod("ExecCommandTaskArray", CommandArg[][].class);

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
	public void VisitCommandNode(CommandNode Node) {
		this.AsmBuilder.SetLineNumber(Node);
		ArrayList<CommandNode> nodeList = new ArrayList<CommandNode>();
		CommandNode node = Node;
		while(node != null) {
			nodeList.add(node);
			node = (CommandNode) node.PipedNextNode;
		}
		// new String[n][]
		int size = nodeList.size();
		this.AsmBuilder.visitLdcInsn(size);
		this.AsmBuilder.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(CommandArg[].class));
		for(int i = 0; i < size; i++) {
			// new String[m];
			CommandNode currentNode = nodeList.get(i);
			int listSize = currentNode.GetArgSize();
			this.AsmBuilder.visitInsn(Opcodes.DUP);
			this.AsmBuilder.visitLdcInsn(i);
			this.AsmBuilder.visitLdcInsn(listSize);
			this.AsmBuilder.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(CommandArg.class));
			for(int j = 0; j < listSize; j++ ) {
				this.AsmBuilder.visitInsn(Opcodes.DUP);
				this.AsmBuilder.visitLdcInsn(j);
				currentNode.GetArgAt(j).Accept(this);
				this.AsmBuilder.visitInsn(Opcodes.AASTORE);
			}
			this.AsmBuilder.visitInsn(Opcodes.AASTORE);
		}

		if(Node.Type.IsBooleanType()) {
			this.invokeStaticMethod(Node, this.ExecCommandBool);
		}
		else if(Node.Type.IsIntType()) {
			this.invokeStaticMethod(Node, this.ExecCommandInt);
		}
		else if(Node.Type.IsStringType()) {
			this.invokeStaticMethod(Node, this.ExecCommandString);
		}
		else if(Node.Type.equals(BTypePool._GetGenericType1(BGenericType._ArrayType, BType.StringType))) {
			this.invokeStaticMethod(Node, this.ExecCommandStringArray);
		}
		else if(Node.Type.equals(JavaTypeTable.GetBunType(Task.class))) {
			this.invokeStaticMethod(Node, this.ExecCommandTask);
		}
		else if(Node.Type.equals(BTypePool._GetGenericType1(BGenericType._ArrayType, JavaTypeTable.GetBunType(Task.class)))) {
			this.invokeStaticMethod(Node, this.ExecCommandTaskArray);
		}
		else {
			this.invokeStaticMethod(Node, this.ExecCommandVoid);
		}
	}

	@Override
	public void VisitTryNode(DShellTryNode Node) {
		AsmTryCatchLabel Label = new AsmTryCatchLabel();
		this.TryCatchLabel.push(Label); // push
		// try block
		this.AsmBuilder.visitLabel(Label.BeginTryLabel);
		Node.TryBlockNode().Accept(this);
		this.AsmBuilder.visitLabel(Label.EndTryLabel);
		this.AsmBuilder.visitJumpInsn(Opcodes.GOTO, Label.FinallyLabel);
		// catch block
		int size = Node.GetListSize();
		for(int i = 0; i < size; i++) {
			Node.GetListAt(i).Accept(this);
		}
		// finally block
		this.AsmBuilder.visitLabel(Label.FinallyLabel);
		if(Node.HasFinallyBlockNode()) {
			Node.FinallyBlockNode().Accept(this);
		}
		this.TryCatchLabel.pop();
	}

	@Override
	public void VisitCatchNode(DShellCatchNode Node) {
		Label catchLabel = new Label();
		AsmTryCatchLabel Label = this.TryCatchLabel.peek();

		// prepare
		String throwType = this.resolveExceptionType(Node);
		this.AsmBuilder.visitTryCatchBlock(Label.BeginTryLabel, Label.EndTryLabel, catchLabel, throwType);

		// catch block
		this.AsmBuilder.AddLocal(this.GetJavaClass(Node.ExceptionType()), Node.ExceptionName());
		this.AsmBuilder.visitLabel(catchLabel);
		this.invokeExceptionWrapper(Node);
		Node.BlockNode().Accept(this);
		this.AsmBuilder.visitJumpInsn(Opcodes.GOTO, Label.FinallyLabel);

		this.AsmBuilder.RemoveLocal(this.GetJavaClass(Node.ExceptionType()), Node.ExceptionName());
	}

	private String resolveExceptionType(DShellCatchNode Node) {
		if(!Node.HasTypeInfo()) {
			return Type.getType(Throwable.class).getInternalName();
		}
		return this.AsmType(Node.ExceptionType()).getInternalName();
	}

	private void invokeExceptionWrapper(DShellCatchNode Node) {
		if(!Node.HasTypeInfo()) {
			this.invokeStaticMethod(null, this.wrapException);
		}
		this.AsmBuilder.StoreLocal(Node.ExceptionName());
	}

	@Override public void VisitThrowNode(BunThrowNode Node) {
		Node.ExprNode().Accept(this);
		this.AsmBuilder.visitInsn(Opcodes.ATHROW);
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		if(!(Node.LeftNode().Type instanceof BGenericType) && !(Node.LeftNode().Type instanceof BFuncType)) {
			this.VisitNativeInstanceOfNode(Node);
			return;
		}
		Node.LeftNode().Accept(this);
		this.AsmBuilder.Pop(Node.LeftNode().Type);
		this.AsmBuilder.PushLong(Node.LeftNode().Type.TypeId);
		this.AsmBuilder.PushLong(Node.TargetType().TypeId);
		Method method = JavaMethodTable.GetBinaryStaticMethod(BType.IntType, "==", BType.IntType);
		this.invokeStaticMethod(null, method);
	}

	private void VisitNativeInstanceOfNode(BunInstanceOfNode Node) {
		if(!Node.TargetType().Equals(JavaTypeTable.GetBunType(this.GetJavaClass(Node.TargetType())))) {
			Node.LeftNode().Accept(this);
			this.AsmBuilder.Pop(Node.LeftNode().Type);
			this.AsmBuilder.PushBoolean(false);
			return;
		}
		Class<?> JavaClass = this.GetJavaClass(Node.TargetType());
		if(Node.TargetType().IsIntType()) {
			JavaClass = Long.class;
		}
		else if(Node.TargetType().IsFloatType()) {
			JavaClass = Double.class;
		}
		else if(Node.TargetType().IsBooleanType()) {
			JavaClass = Boolean.class;
		}
		this.invokeBoxingMethod(Node.LeftNode());
		this.AsmBuilder.visitTypeInsn(Opcodes.INSTANCEOF, JavaClass);
	}

	private void invokeBoxingMethod(BNode TargetNode) {
		Class<?> TargetClass = Object.class;
		if(TargetNode.Type.IsIntType()) {
			TargetClass = Long.class;
		}
		else if(TargetNode.Type.IsFloatType()) {
			TargetClass = Double.class;
		}
		else if(TargetNode.Type.IsBooleanType()) {
			TargetClass = Boolean.class;
		}
		Class<?> SourceClass = this.GetJavaClass(TargetNode.Type);
		Method sMethod = JavaMethodTable.GetCastMethod(TargetClass, SourceClass);
		TargetNode.Accept(this);
		if(!TargetClass.equals(Object.class)) {
			this.invokeStaticMethod(null, sMethod);
		}
	}

	@Override public void VisitErrorNode(ErrorNode Node) {
		assert Node.SourceToken != null;
		LibBunLogger._LogError(Node.SourceToken, Node.ErrorMessage);
		throw new ErrorNodeFoundException();
	}

	@Override public void VisitSyntaxSugarNode(SyntaxSugarNode Node) {
		if(Node instanceof BunContinueNode) {
			this.VisitContinueNode((BunContinueNode) Node);
		}
		else {
			super.VisitSyntaxSugarNode(Node);
		}
	}

	@Override
	public void VisitContinueNode(BunContinueNode Node) {
		Label l = this.AsmBuilder.ContinueLabelStack.peek();
		this.AsmBuilder.visitJumpInsn(Opcodes.GOTO, l);
	}

	@Override
	public void VisitForNode(DShellForNode Node) {
		Label headLabel = new Label();
		Label continueLabel = new Label();
		Label breakLabel = new Label();
		this.AsmBuilder.BreakLabelStack.push(breakLabel);
		this.AsmBuilder.ContinueLabelStack.push(continueLabel);

		if(Node.HasDeclNode()) {
			this.VisitVarDeclNode(Node.VarDeclNode());
		}
		this.AsmBuilder.visitLabel(headLabel);
		Node.CondNode().Accept(this);
		this.AsmBuilder.visitJumpInsn(Opcodes.IFEQ, breakLabel);
		Node.BlockNode().Accept(this);
		this.AsmBuilder.visitLabel(continueLabel);
		if(Node.HasNextNode()) {
			Node.NextNode().Accept(this);
		}
		this.AsmBuilder.visitJumpInsn(Opcodes.GOTO, headLabel);
		this.AsmBuilder.visitLabel(breakLabel);
		if(Node.HasDeclNode()) {
			this.VisitVarDeclNode2(Node.VarDeclNode());
		}

		this.AsmBuilder.BreakLabelStack.pop();
		this.AsmBuilder.ContinueLabelStack.pop();
	}

	@Override
	protected void VisitVarDeclNode(BunLetVarNode Node) {
		String VarName = Node.GetGivenName();
		if(this.AsmBuilder.FindLocalVariable(VarName) != null) {
			this.VisitErrorNode(new ErrorNode(Node, VarName + " is already defined"));
			return;
		}
		Class<?> DeclClass = this.GetJavaClass(Node.DeclType());
		this.AsmBuilder.AddLocal(DeclClass, VarName);
		this.AsmBuilder.PushNode(DeclClass, Node.InitValueNode());
		this.AsmBuilder.StoreLocal(VarName);
	}

	@Override
	public void VisitWrapperNode(DShellWrapperNode Node) {
		Node.getTargetNode().Accept(this);
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
	public void VisitLetNode(BunLetVarNode Node) {
		String varName = Node.GetGivenName();
		if(GlobalVariableTable.existEntry(varName)) {
			this.VisitErrorNode(new ErrorNode(Node, varName + " is already defined"));
			return;
		}
		int typeId = this.getTypeId(Node.DeclType());
		int varIndex = GlobalVariableTable.addEntry(varName, typeId, Node.IsReadOnly());
		try {
			this.setVariable(varIndex, typeId, Node.InitValueNode());
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
	protected void VisitGlobalNameNode(GetNameNode Node) {
		BunLetVarNode LetNode = Node.ResolvedNode;
		String varName = LetNode.GetGivenName();
		int typeId = this.getTypeId(LetNode.DeclType());
		int varIndex = GlobalVariableTable.getVarIndex(varName, typeId);
		if(varIndex == -1) {
			this.VisitErrorNode(new ErrorNode(Node, "undefiend varibale: " + varName));
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
			this.AsmBuilder.visitTypeInsn(Opcodes.CHECKCAST, this.GetJavaClass(Node.Type));
			break;
		}
	}

	@Override
	protected void GenerateAssignNode(GetNameNode Node, BNode ExprNode) {
		if(Node.ResolvedNode == null) {
			this.VisitErrorNode(new ErrorNode(Node, "undefined symbol: " + Node.GivenName));
			return;
		}
		if(Node.ResolvedNode.IsReadOnly() && !(Node.ResolvedNode.ParentNode instanceof BunFunctionNode)) {
			this.VisitErrorNode(new ErrorNode(Node, "read only variable: " + Node.GivenName));
			return;
		}
		if(Node.ResolvedNode.GetDefiningFunctionNode() == null) {
			int typeId = this.getTypeId(ExprNode.Type);
			int varIndex = GlobalVariableTable.getVarIndex(Node.ResolvedNode.GetGivenName(), typeId);
			this.setVariable(varIndex, typeId, ExprNode);
			return;
		}
		String Name = Node.GetUniqueName(this);
		this.AsmBuilder.PushNode(this.AsmBuilder.GetLocalType(Name), ExprNode);
		this.AsmBuilder.StoreLocal(Name);
	}

	@Override
	public void VisitMatchRegexNode(MatchRegexNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override
	public void VisitInternalFuncCallNode(InternalFuncCallNode Node) {
		this.invokeStaticMethod(null, Node.getMethod());
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
		StringBuilder ARGVBuilder = new StringBuilder();
		ARGVBuilder.append("let ARGV = [");
		for(int i = 0; i < scriptArgs.length; i++) {
			if(i != 0) {
				ARGVBuilder.append(", ");
			}
			ARGVBuilder.append("\"");
			ARGVBuilder.append(scriptArgs[i]);
			ARGVBuilder.append("\"");
		}
		ARGVBuilder.append("]");
		this.loadScript(ARGVBuilder.toString(), scriptArgs[0], 0, false);
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

	protected void generateByteCode(BNode Node) {
		try {
			Node.Accept(this);
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

	protected boolean generateStatement(BNode Node, boolean IsInteractive) {
		this.EnableVisitor();
		if(Node instanceof EmptyNode) {
			return this.IsVisitable();
		}
		if(Node instanceof TopLevelNode) {
			((TopLevelNode)Node).Perform(this.RootGamma);
			return this.IsVisitable();
		}
		Node = this.checkTopLevelSupport(Node);
		if(Node.IsErrorNode()) {
			this.generateByteCode(Node);
		}
		else if(IsInteractive && (Node instanceof DShellWrapperNode) && !((DShellWrapperNode)Node).isVarTarget()) {
			Node = this.TypeChecker.CheckType(Node, BType.VoidType);
			this.generateByteCode(Node);
		}
		else if(IsInteractive) {
			BunFunctionNode FuncNode = ((DShellTypeChecker)this.TypeChecker).VisitTopLevelStatementNode(Node);
			this.topLevelStatementList.add(new TopLevelStatementInfo(FuncNode.GivenName, FuncNode.ReturnType()));
			this.generateByteCode(FuncNode);
		}
		else {
			if(this.untypedMainNode == null) {
				this.untypedMainNode = new BunFunctionNode(Node.ParentNode);
				this.untypedMainNode.GivenName = "main";
				this.untypedMainNode.SourceToken = Node.SourceToken;
				this.untypedMainNode.SetNode(BunFunctionNode._Block, new BunBlockNode(this.untypedMainNode, null));
			}
			this.untypedMainNode.BlockNode().Append(Node);
		}
		return this.IsVisitable();
	}

	protected boolean evalAndPrintEachNode(TopLevelStatementInfo info) {
		Class<?> FuncClass = this.GetDefinedFunctionClass(info.funcName, BType.VoidType, 0);
		try {
			Method Method = FuncClass.getMethod("f");
			Object Value = Method.invoke(null);
			if(!info.returnType.IsVoidType()) {
				System.out.println(" (" + info.returnType + ") " + Value);
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
			BunFunctionNode Node = (BunFunctionNode) this.TypeChecker.CheckType(this.untypedMainNode, BType.VarType);
			Node.Type = BType.VoidType;
			Node.IsExport = true;
			Node.Accept(this);
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
			JavaStaticFieldNode MainFunc = this.MainFuncNode;
			try {
				Method Method = MainFunc.StaticClass.getMethod("f");
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
