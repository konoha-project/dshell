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
import libbun.ast.expression.SetNameNode;
import libbun.ast.statement.BunReturnNode;
import libbun.ast.statement.BunThrowNode;
import libbun.ast.sugar.BunContinueNode;
import libbun.encode.jvm.JavaMethodTable;
import libbun.encode.jvm.JavaTypeTable;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import dshell.ast.DShellCatchNode;
import dshell.ast.DShellForNode;
import dshell.ast.DShellTryNode;
import dshell.ast.DShellWrapperNode;
import dshell.exception.DShellException;
import dshell.exception.Errno;
import dshell.exception.MultipleException;
import dshell.lang.DShellTypeChecker;
import dshell.lang.DShellVisitor;
import dshell.lib.CommandArg;
import dshell.lib.CommandArg.SubstitutedArg;
import dshell.lib.GlobalVariableTable;
import dshell.lib.Task;
import dshell.lib.TaskBuilder;
import dshell.lib.Utils;
import dshell.lib.ArrayUtils.DShellExceptionArray;
import dshell.lib.ArrayUtils.TaskArray;
import libbun.lang.bun.shell.CommandNode;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.parser.LibBunLogger;
import libbun.type.BFormFunc;
import libbun.type.BFuncType;
import libbun.type.BGenericType;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.util.BArray;
import libbun.util.BLib;

public class DShellByteCodeGenerator extends AsmJavaGenerator implements DShellVisitor {
	private BunFunctionNode untypedMainNode = null;
	private LinkedList<String> topLevelSymbolList;

	private Method ExecCommandVoid;
	private Method ExecCommandBool;
	private Method ExecCommandInt;
	private Method ExecCommandString;
	private Method ExecCommandStringArray;
	private Method ExecCommandTask;
	private Method ExecCommandTaskArray;

	public DShellByteCodeGenerator() {
		super();
		this.topLevelSymbolList = new LinkedList<String>();
		this.loadJavaClass(Task.class);
		this.loadJavaClass(DShellException.class);
		this.loadJavaClass(MultipleException.class);
		this.loadJavaClass(Errno.UnimplementedErrnoException.class);
		this.loadJavaClass(DShellException.NullException.class);
		this.loadJavaClassList(Errno.getExceptionClassList());
		this.loadJavaClass(CommandArg.class);
		this.loadJavaClass(SubstitutedArg.class);

		try {
			this.ExecCommandVoid = TaskBuilder.class.getMethod("ExecCommandVoid", CommandArg[][].class);
			this.ExecCommandBool = TaskBuilder.class.getMethod("ExecCommandBool", CommandArg[][].class);
			this.ExecCommandInt = TaskBuilder.class.getMethod("ExecCommandInt", CommandArg[][].class);
			this.ExecCommandString = TaskBuilder.class.getMethod("ExecCommandString", CommandArg[][].class);
			this.ExecCommandStringArray = TaskBuilder.class.getMethod("ExecCommandStringArray", CommandArg[][].class);
			this.ExecCommandTask = TaskBuilder.class.getMethod("ExecCommandTask", CommandArg[][].class);
			this.ExecCommandTaskArray = TaskBuilder.class.getMethod("ExecCommandTaskArray", CommandArg[][].class);
		}
		catch(Exception e) {
			e.printStackTrace();
			Utils.fatal(1, "method loading failed");
		}
		JavaMethodTable.Import(BType.StringType, "=~", BType.StringType, Utils.class, "matchRegex");
		JavaMethodTable.Import(BType.StringType, "!~", BType.StringType, Utils.class, "unmatchRegex");

		// load array class
		this.loadArrayClass(DShellException.class, DShellExceptionArray.class);
		this.loadArrayClass(Task.class, TaskArray.class);

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
		else if(Node.Type.equals(JavaTypeTable.GetZenType(Task.class))) {
			this.invokeStaticMethod(Node, this.ExecCommandTask);
		}
		else if(Node.Type.equals(BTypePool._GetGenericType1(BGenericType._ArrayType, JavaTypeTable.GetZenType(Task.class)))) {
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
		String throwType = this.AsmType(Node.ExceptionType()).getInternalName();
		this.AsmBuilder.visitTryCatchBlock(Label.BeginTryLabel, Label.EndTryLabel, catchLabel, throwType);

		// catch block
		this.AsmBuilder.AddLocal(this.GetJavaClass(Node.ExceptionType()), Node.ExceptionName());
		this.AsmBuilder.visitLabel(catchLabel);
		this.AsmBuilder.StoreLocal(Node.ExceptionName());
		Node.BlockNode().Accept(this);
		this.AsmBuilder.visitJumpInsn(Opcodes.GOTO, Label.FinallyLabel);

		this.AsmBuilder.RemoveLocal(this.GetJavaClass(Node.ExceptionType()), Node.ExceptionName());
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
		if(!Node.TargetType().Equals(JavaTypeTable.GetZenType(this.GetJavaClass(Node.TargetType())))) {
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
		else if(Node instanceof CommandNode) {
			this.VisitCommandNode((CommandNode) Node);
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
		this.setVariable(varIndex, typeId, Node.InitValueNode());
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
	public void VisitSetNameNode(SetNameNode Node) {
		GetNameNode NameNode = Node.NameNode();
		if(NameNode.ResolvedNode == null) {
			this.VisitErrorNode(new ErrorNode(Node, "undefined symbol: " + NameNode.GivenName));
			return;
		}
		if(NameNode.ResolvedNode.IsReadOnly() && !(NameNode.ResolvedNode.ParentNode instanceof BunFunctionNode)) {
			this.VisitErrorNode(new ErrorNode(Node, "read only variable: " + NameNode.GivenName));
			return;
		}
		if(NameNode.ResolvedNode.GetDefiningFunctionNode() == null) {
			int typeId = this.getTypeId(Node.ExprNode().Type);
			int varIndex = GlobalVariableTable.getVarIndex(NameNode.ResolvedNode.GetGivenName(), typeId);
			this.setVariable(varIndex, typeId, Node.ExprNode());
			return;
		}
		String Name = NameNode.GetUniqueName(this);
		this.AsmBuilder.PushNode(this.AsmBuilder.GetLocalType(Name), Node.ExprNode());
		this.AsmBuilder.StoreLocal(Name);
	}

	private void invokeStaticMethod(BNode Node, Method method) {
		String owner = Type.getInternalName(method.getDeclaringClass());
		this.AsmBuilder.visitMethodInsn(Opcodes.INVOKESTATIC, owner, method.getName(), Type.getMethodDescriptor(method));
		if(Node != null) {
			this.AsmBuilder.CheckReturnCast(Node, method.getReturnType());
		}
	}

	private void loadJavaClass(Class<?> classObject) {
		BType type = JavaTypeTable.GetZenType(classObject);
		this.RootGamma.SetTypeName(type, null);
	}

	private void loadJavaClassList(ArrayList<Class<?>> classObjList) {
		for(Class<?> classObj : classObjList) {
			this.loadJavaClass(classObj);
		}
	}

	private void loadJavaStaticMethod(Class<?> holderClass, String internalName, Class<?>... paramClasses) {
		this.loadJavaStaticMethod(holderClass, internalName, internalName, paramClasses);
	}

	private void loadJavaStaticMethod(Class<?> holderClass, String name, String internalName, Class<?>... paramClasses) {
		String macroSymbol = name;
		String holderClassPath = holderClass.getCanonicalName().replaceAll("\\.", "/");
		BArray<BType> typeList = new BArray<BType>(new BType[4]);
		StringBuilder macroBuilder = new StringBuilder();
		macroBuilder.append(holderClassPath + "." + internalName + "(");
		for(int i = 0; i < paramClasses.length; i++) {
			if(i != 0) {
				macroBuilder.append(",");
			}
			macroBuilder.append("$[" + i + "]");
			typeList.add(JavaTypeTable.GetZenType(paramClasses[i]));
		}
		macroBuilder.append(")");
		try {
			typeList.add(JavaTypeTable.GetZenType(holderClass.getMethod(internalName, paramClasses).getReturnType()));
			BFuncType macroType = (BFuncType) BTypePool._GetGenericType(BFuncType._FuncType, typeList, true);
			BFormFunc macroFunc = new BFormFunc(macroSymbol, macroType, null, macroBuilder.toString());
			if(name.equals("_")) {
				this.SetConverterFunc(macroType.GetRecvType(), macroType.GetReturnType(), macroFunc);
			}
			else {
				this.SetDefinedFunc(macroFunc);
			}
		}
		catch(Exception e) {
			Utils.fatal(1, "load static method faild: " + e.getMessage());
		}
	}

	private void loadArrayClass(Class<?> baseClass, Class<?> arrayClass) {
		BType baseType = JavaTypeTable.GetZenType(baseClass);
		BType arrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, baseType);
		JavaTypeTable.SetTypeTable(arrayType, arrayClass);
		// define operator
		JavaMethodTable.Import(arrayType, "[]", BType.IntType, arrayClass, "GetIndex");
	}

	protected boolean loadScript(String script, String fileName, int lineNumber, boolean isInteractive) {
		boolean result = true;
		BunBlockNode topBlockNode = new BunBlockNode(null, this.RootGamma);
		BTokenContext tokenContext = new BTokenContext(this, this.RootGamma, fileName, lineNumber, script);
		tokenContext.SkipEmptyStatement();
		BToken skipToken = tokenContext.GetToken();
		while(tokenContext.HasNext()) {
			tokenContext.SetParseFlag(BTokenContext._NotAllowSkipIndent);
			topBlockNode.ClearListToSize(0);
			skipToken = tokenContext.GetToken();
			BNode stmtNode;
			try {
				stmtNode = tokenContext.ParsePattern(topBlockNode, "$Statement$", BTokenContext._Required);
			}
			catch(Exception e) {
				System.err.println("Parsing Problem");
				e.printStackTrace();
				this.topLevelSymbolList.clear();
				return false;
			}
			if(stmtNode.IsErrorNode()) {
				tokenContext.SkipError(skipToken);
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
		String script = BLib._LoadTextFile(fileName);
		if(script == null) {
			System.err.println("file not found: " + fileName);
			System.exit(1);
		}
		return this.loadScript(script, fileName, 1, false);
	}

	protected void generateByteCode(BNode Node) {
		try {
			Node.Accept(this);
		}
		catch(ErrorNodeFoundException e) {
			this.topLevelSymbolList.clear();
			this.StopVisitor();
		}
		catch(Exception e) {
			System.err.println("Code Generation Failed");
			e.printStackTrace();
			this.topLevelSymbolList.clear();
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
		if(IsInteractive && (Node instanceof DShellWrapperNode) && !((DShellWrapperNode)Node).isVarTarget()) {
			Node = this.TypeChecker.CheckType(Node, BType.VoidType);
			this.generateByteCode(Node);
		}
		else if(IsInteractive) {
			BunFunctionNode FuncNode = ((DShellTypeChecker)this.TypeChecker).VisitTopLevelStatementNode(Node);
			this.topLevelSymbolList.add(FuncNode.GivenName);
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

	private boolean evalAndPrintEachNode(String Symbol) {
		Class<?> FuncClass = this.GetDefinedFunctionClass(Symbol, BType.VoidType, 0);
		try {
			Method Method = FuncClass.getMethod("f");
			Object Value = Method.invoke(null);
			if(Method.getReturnType() != void.class) {
				System.out.println(" (" + Method.getReturnType().getSimpleName() + ") " + Value);
			}
			return true;
		}
		catch(InvocationTargetException e) {
			this.printException(e);
		}
		catch(Exception e) {
			e.printStackTrace();
			Utils.fatal(1, "invocation problem");
		}
		return false;
	}

	public void evalAndPrint() {
		while(!this.topLevelSymbolList.isEmpty()) {
			String Symbol = this.topLevelSymbolList.remove();
			if(!this.evalAndPrintEachNode(Symbol)) {
				this.topLevelSymbolList.clear();
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
			System.exit(1);
		}
		catch(Exception e) {
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
			catch(Exception e) {
				e.printStackTrace();
				Utils.fatal(1, "invocation problem");
			}
		}
	}

	private BNode checkTopLevelSupport(BNode Node) {
		if(Node instanceof BunVarBlockNode) {
			return new ErrorNode(Node, "only available inside function");
		}
		else if(Node instanceof BunClassNode || Node instanceof BunFunctionNode || Node instanceof BunLetVarNode) {
			return new DShellWrapperNode(Node);
		}
		BunReturnNode ReturnNode = this.findReturnNode(Node);
		if(ReturnNode != null) {
			return new ErrorNode(ReturnNode, "only available inside function");
		}
		return Node;
	}

	private BunReturnNode findReturnNode(BNode Node) {
		if(Node == null) {
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
		Throwable cause = e.getCause();
		if(cause instanceof DShellException) {
			cause.printStackTrace();
		}
		else {
			System.err.println(cause);
			if(BLib.DebugMode) {
				cause.printStackTrace();
			}
		}
	}

	private static class ErrorNodeFoundException extends RuntimeException {
		private static final long serialVersionUID = -2465006344250569543L;
	}
}
