package zen.codegen.jvm;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.INSTANCEOF;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import dshell.ast.DShellCatchNode;
import dshell.ast.DShellCommandNode;
import dshell.ast.DShellDummyNode;
import dshell.ast.DShellTryNode;
import dshell.exception.DShellException;
import dshell.exception.Errno;
import dshell.exception.MultipleException;
import dshell.lang.DShellVisitor;
import dshell.lib.DShellExceptionArray;
import dshell.lib.Task;
import dshell.lib.TaskBuilder;
import dshell.lib.Utils;
import dshell.remote.TaskArray;
import zen.ast.ZBlockNode;
import zen.ast.ZClassNode;
import zen.ast.ZFunctionNode;
import zen.ast.ZInstanceOfNode;
import zen.ast.ZLetNode;
import zen.ast.ZNode;
import zen.ast.ZThrowNode;
import zen.ast.ZTopLevelNode;
import zen.ast.ZVarNode;
import zen.codegen.jvm.JavaMethodTable;
import zen.codegen.jvm.JavaTypeTable;
import zen.codegen.jvm.TryCatchLabel;
import zen.util.LibZen;
import zen.util.Var;
import zen.util.ZArray;
import zen.type.ZFuncType;
import zen.type.ZGenericType;
import zen.type.ZType;
import zen.type.ZTypePool;

public class ModifiedAsmGenerator extends AsmJavaGenerator implements DShellVisitor {
	private ZFunctionNode untypedMainNode = null;
	private LinkedList<String> topLevelSymbolList;
	
	private Method ExecCommandVoid;
	private Method ExecCommandBool;
	private Method ExecCommandInt;
	private Method ExecCommandString;
	private Method ExecCommandTask;
	private Method ExecCommandTaskArray;

	public ModifiedAsmGenerator() {
		super();
		this.topLevelSymbolList = new LinkedList<String>();
		this.importJavaClass(Task.class);
		this.importJavaClass(DShellException.class);
		this.importJavaClass(MultipleException.class);
		this.importJavaClass(Errno.UnimplementedErrnoException.class);
		this.importJavaClass(DShellException.NullException.class);
		this.importJavaClassList(Errno.getExceptionClassList());

		try {
			ExecCommandVoid = TaskBuilder.class.getMethod("ExecCommandVoid", String[][].class);
			ExecCommandBool = TaskBuilder.class.getMethod("ExecCommandBool", String[][].class);
			ExecCommandInt = TaskBuilder.class.getMethod("ExecCommandInt", String[][].class);
			ExecCommandString = TaskBuilder.class.getMethod("ExecCommandString", String[][].class);
			ExecCommandTask = TaskBuilder.class.getMethod("ExecCommandTask", String[][].class);
			ExecCommandTaskArray = TaskBuilder.class.getMethod("ExecCommandTaskArray", String[][].class);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.err.println("method loading failed");
			System.exit(1);
		}
		JavaMethodTable.Import(ZType.StringType, "=~", ZType.StringType, Utils.class, "matchRegex");
		JavaMethodTable.Import(ZType.StringType, "!~", ZType.StringType, Utils.class, "unmatchRegex");

		// load exception array
		ZType DShellExceptionType = JavaTypeTable.GetZenType(DShellException.class);
		ZType DShellExceptionArrayType = ZTypePool._GetGenericType1(ZGenericType._ArrayType, DShellExceptionType);
		JavaTypeTable.SetTypeTable(DShellExceptionArrayType, DShellExceptionArray.class);

		JavaMethodTable.Import(DShellExceptionArrayType, "[]", ZType.IntType, DShellExceptionArray.class, "GetIndex");

		// load task array
		ZType TaskType = JavaTypeTable.GetZenType(Task.class);
		ZType TaskArrayType = ZTypePool._GetGenericType1(ZGenericType._ArrayType, TaskType);
		JavaTypeTable.SetTypeTable(TaskArrayType, TaskArray.class);

		JavaMethodTable.Import(TaskArrayType, "[]", ZType.IntType, TaskArray.class, "GetIndex");
		JavaMethodTable.Import(TaskArrayType, "[]=", ZType.IntType, TaskArray.class, "SetIndex", Task.class);

		// load static method
		this.loadJavaStaticMethod(Utils.class, "getEnv", String.class);
		this.loadJavaStaticMethod(Utils.class, "setEnv", String.class, String.class);
	}

	@Override
	public void VisitCommandNode(DShellCommandNode Node) {
		this.AsmBuilder.SetLineNumber(Node);
		ArrayList<DShellCommandNode> nodeList = new ArrayList<DShellCommandNode>();
		DShellCommandNode node = Node;
		while(node != null) {
			nodeList.add(node);
			node = (DShellCommandNode) node.PipedNextNode;
		}
		// new String[n][]
		int size = nodeList.size();
		this.AsmBuilder.visitLdcInsn(size);
		this.AsmBuilder.visitTypeInsn(ANEWARRAY, Type.getInternalName(String[].class));
		for(int i = 0; i < size; i++) {
			// new String[m];
			DShellCommandNode currentNode = nodeList.get(i);
			int listSize = currentNode.GetListSize();
			this.AsmBuilder.visitInsn(DUP);
			this.AsmBuilder.visitLdcInsn(i);
			this.AsmBuilder.visitLdcInsn(listSize);
			this.AsmBuilder.visitTypeInsn(ANEWARRAY, Type.getInternalName(String.class));
			for(int j = 0; j < listSize; j++ ) {
				this.AsmBuilder.visitInsn(DUP);
				this.AsmBuilder.visitLdcInsn(j);
				currentNode.GetListAt(j).Accept(this);
				this.AsmBuilder.visitInsn(AASTORE);
			}
			this.AsmBuilder.visitInsn(AASTORE);
		}
		
		if(Node.Type.IsBooleanType()) {
			this.invokeStaticMethod(Node.Type, ExecCommandBool);
		}
		else if(Node.Type.IsIntType()) {
			this.invokeStaticMethod(Node.Type, ExecCommandInt);
		}
		else if(Node.Type.IsStringType()) {
			this.invokeStaticMethod(Node.Type, ExecCommandString);
		}
		else if(this.GetJavaClass(Node.Type).equals(Task.class)) {
			this.invokeStaticMethod(Node.Type, ExecCommandTask);
		}
		else if(this.GetJavaClass(Node.Type).equals(TaskArray.class)) {
			this.invokeStaticMethod(Node.Type, ExecCommandTaskArray);
		}
		else {
			this.invokeStaticMethod(Node.Type, ExecCommandVoid);
		}
	}

	@Override
	public void VisitTryNode(DShellTryNode Node) {
		TryCatchLabel Label = new TryCatchLabel();
		this.TryCatchLabel.push(Label); // push
		// try block
		this.AsmBuilder.visitLabel(Label.beginTryLabel);
		Node.TryBlockNode().Accept(this);
		this.AsmBuilder.visitLabel(Label.endTryLabel);
		this.AsmBuilder.visitJumpInsn(GOTO, Label.finallyLabel);
		// catch block
		int size = Node.GetListSize();
		for(int i = 0; i < size; i++) {
			Node.GetListAt(i).Accept(this);
		}
		// finally block
		this.AsmBuilder.visitLabel(Label.finallyLabel);
		if(Node.HasFinallyBlockNode()) {
			Node.FinallyBlockNode().Accept(this);
		}
		this.TryCatchLabel.pop();
	}

	@Override
	public void VisitCatchNode(DShellCatchNode Node) {
		Label catchLabel = new Label();
		TryCatchLabel Label = this.TryCatchLabel.peek();

		// prepare
		String throwType = this.AsmType(Node.ExceptionType()).getInternalName();
		this.AsmBuilder.visitTryCatchBlock(Label.beginTryLabel, Label.endTryLabel, catchLabel, throwType);

		// catch block
		this.AsmBuilder.AddLocal(this.GetJavaClass(Node.ExceptionType()), Node.ExceptionName());
		this.AsmBuilder.visitLabel(catchLabel);
		this.AsmBuilder.StoreLocal(Node.ExceptionName());
		Node.BlockNode().Accept(this);
		this.AsmBuilder.visitJumpInsn(GOTO, Label.finallyLabel);

		this.AsmBuilder.RemoveLocal(this.GetJavaClass(Node.ExceptionType()), Node.ExceptionName());
	}

	@Override public void VisitThrowNode(ZThrowNode Node) {
		Node.ExprNode().Accept(this);
		this.AsmBuilder.visitInsn(ATHROW);
	}

	@Override public void VisitInstanceOfNode(ZInstanceOfNode Node) {
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

		ZNode TargetNode = Node.LeftNode();
		if(TargetNode.Type.IsIntType() || TargetNode.Type.IsFloatType() || TargetNode.Type.IsBooleanType()) {
			this.invokeBoxingMethod(TargetNode);
		}
		else {
			TargetNode.Accept(this);
		}
		this.AsmBuilder.visitTypeInsn(INSTANCEOF, JavaClass);
	}

	@Override
	public void VisitDummyNode(DShellDummyNode Node) {	// do nothing
	}

	private void invokeBoxingMethod(ZNode TargetNode) {
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
		this.invokeStaticMethod(ZType.BooleanType, sMethod);
	}

	private void invokeStaticMethod(ZType type, Method method) { //TODO: check return type cast
		String owner = Type.getInternalName(method.getDeclaringClass());
		this.AsmBuilder.visitMethodInsn(INVOKESTATIC, owner, method.getName(), Type.getMethodDescriptor(method));
	}

	private void importJavaClass(Class<?> classObject) {
		ZType type = JavaTypeTable.GetZenType(classObject);
		this.RootNameSpace.SetTypeName(type, null);
	}

	private void importJavaClassList(ArrayList<Class<?>> classObjList) {
		for(Class<?> classObj : classObjList) {
			this.importJavaClass(classObj);
		}
	}

	private void loadJavaStaticMethod(Class<?> holderClass, String internalName, Class<?>... paramClasses) {
		this.loadJavaStaticMethod(holderClass, internalName, internalName, paramClasses);
	}

	private void loadJavaStaticMethod(Class<?> holderClass, String name, String internalName, Class<?>... paramClasses) {
		String macroSymbol = name;
		String holderClassPath = holderClass.getCanonicalName().replaceAll("\\.", "/");
		ZArray<ZType> typeList = new ZArray<ZType>(new ZType[4]);
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
			ZFuncType macroType = (ZFuncType) ZTypePool._GetGenericType(ZFuncType._FuncType, typeList, true);
			this.SetAsmMacro(this.RootNameSpace, macroSymbol, macroType, macroBuilder.toString());
		}
		catch(Exception e) {
			Utils.fatal(1, "load static method faild: " + e.getMessage());
		}
	}

	@Override
	protected boolean ExecStatement(ZNode Node, boolean IsInteractive) {
		this.EnableVisitor();
		if(Node instanceof ZTopLevelNode) {
			((ZTopLevelNode)Node).Perform(this.RootNameSpace);
		}
		else {
			if(this.IsVisitable()) {
				if(Node instanceof ZFunctionNode || Node instanceof ZClassNode || Node instanceof ZLetNode) {
					Node = this.TypeChecker.CheckType(Node, ZType.VarType);
					Node.Type = ZType.VoidType;
					this.GenerateStatement(Node);
				}
				else if(Node instanceof ZVarNode) {
					System.err.println("unsupprt top level var decl");
					this.StopVisitor();
				}
				else {
					if(IsInteractive) {
						Node = this.TypeChecker.CheckType(Node, ZType.VarType);
						if(!this.LangInfo.AllowTopLevelScript) {
							@Var String FuncName = this.NameUniqueSymbol("Main");
							Node = this.TypeChecker.CreateFunctionNode(Node.ParentNode, FuncName, Node);
							this.topLevelSymbolList.add(FuncName);
						}
						this.GenerateStatement(Node);
					}
					else {
						if(this.untypedMainNode == null) {
							this.untypedMainNode = new ZFunctionNode(Node.ParentNode);
							this.untypedMainNode.GivenName = "main";
							this.untypedMainNode.SetNode(ZFunctionNode._Block, new ZBlockNode(this.untypedMainNode, null));
						}
						this.untypedMainNode.BlockNode().Append(Node);
					}
				}
			}
		}
		return this.IsVisitable();
	}

	@Override public void Perform() {
		if(this.TopLevelSymbol != null) {
			Class<?> FuncClass = this.GetDefinedFunctionClass(this.TopLevelSymbol, ZType.VoidType, 0);
			try {
				Method Method = FuncClass.getMethod("f");
				Object Value = Method.invoke(null);
				if(Method.getReturnType() != void.class) {
					System.out.println(" (" + Method.getReturnType().getSimpleName() + ") " + Value);
				}
			}
			catch(InvocationTargetException e) {
				System.err.println("invocation error: " + e.getCause());
				if(LibZen.DebugMode) {
					e.getCause().printStackTrace();
				}
			}
			catch(Exception e) {
				System.err.println("invocation error: " + e);
				if(LibZen.DebugMode) {
					e.printStackTrace();
				}
			}
		}
	}

	public void EvalAndPrint() {
		this.TopLevelSymbol = null;
		while(!this.topLevelSymbolList.isEmpty()) {
			this.TopLevelSymbol = this.topLevelSymbolList.remove();
			this.Perform();
			this.TopLevelSymbol = null;
		}
	}

	public void InvokeMain() {	//TODO
		if(this.untypedMainNode == null) {
			System.err.println("not found main");
			return;
		}
		ZFunctionNode Node = (ZFunctionNode) this.TypeChecker.CheckType(this.untypedMainNode, ZType.VarType);
		Node.IsExport = true;
		Node.Accept(this);
		this.Logger.OutputErrorsToStdErr();
		if(this.MainFuncNode != null) {
			JavaStaticFieldNode MainFunc = this.MainFuncNode;
			try {
				Method Method = MainFunc.StaticClass.getMethod("f");
				Method.invoke(null);
			}
			catch(InvocationTargetException e) {
				System.err.println("invocation error: " + e.getCause());
				if(LibZen.DebugMode) {
					e.getCause().printStackTrace();
				}
			}
			catch(Exception e) {
				System.err.println("invocation error: " + e);
				if(LibZen.DebugMode) {
					e.printStackTrace();
				}
			}
		}
	}
}
