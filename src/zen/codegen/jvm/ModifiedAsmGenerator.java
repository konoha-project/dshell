package zen.codegen.jvm;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.INSTANCEOF;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import dshell.ast.DShellCatchNode;
import dshell.ast.DShellCommandNode;
import dshell.ast.DShellDummyNode;
import dshell.ast.DShellTryNode;
import dshell.exception.DShellException;
import dshell.exception.MultipleException;
import dshell.exception.UnimplementedErrnoException;
import dshell.lang.DShellGrammar;
import dshell.lang.ModifiedTypeSafer;
import dshell.lib.ClassListLoader;
import dshell.lib.DShellExceptionArray;
import dshell.lib.Task;
import dshell.lib.TaskBuilder;
import dshell.util.Utils;
import zen.ast.ZCatchNode;
import zen.ast.ZInstanceOfNode;
import zen.ast.ZNode;
import zen.ast.ZThrowNode;
import zen.codegen.jvm.JavaAsmGenerator;
import zen.codegen.jvm.JavaMethodTable;
import zen.codegen.jvm.JavaTypeTable;
import zen.codegen.jvm.TryCatchLabel;
import zen.parser.ZSourceEngine;
import zen.deps.LibZen;
import zen.parser.ZNameSpace;
import zen.type.ZFunc;
import zen.type.ZType;
import zen.type.ZTypePool;

public class ModifiedAsmGenerator extends JavaAsmGenerator {
	private Method ExecCommandVoid;
	private Method ExecCommandBool;
	private Method ExecCommandInt;
	private Method ExecCommandString;
	private Method ExecCommandTask;

	public ModifiedAsmGenerator() {
		super();
		this.importJavaClass(Task.class);
		this.importJavaClass(DShellException.class);
		this.importJavaClass(MultipleException.class);
		this.importJavaClass(UnimplementedErrnoException.class);
		this.importJavaClass(DShellException.NullException.class);
		this.importJavaClassList(new ClassListLoader("dshell.exception.errno").loadClassList());

		try {
			ExecCommandVoid = TaskBuilder.class.getMethod("ExecCommandVoid", String[][].class);
			ExecCommandBool = TaskBuilder.class.getMethod("ExecCommandBool", String[][].class);
			ExecCommandInt = TaskBuilder.class.getMethod("ExecCommandInt", String[][].class);
			ExecCommandString = TaskBuilder.class.getMethod("ExecCommandString", String[][].class);
			ExecCommandTask = TaskBuilder.class.getMethod("ExecCommandTask", String[][].class);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.err.println("method loading failed");
			System.exit(1);
		}
		JavaMethodTable.Import(ZType.StringType, "=~", ZType.StringType, Utils.class, "matchRegex");
		JavaMethodTable.Import(ZType.StringType, "!~", ZType.StringType, Utils.class, "unmatchRegex");

		ZType DShellExceptionType = JavaTypeTable.GetZenType(DShellException.class);
		ZType DShellExceptionArrayType = ZTypePool._GetGenericType1(ZType.ArrayType, DShellExceptionType);
		JavaTypeTable.SetTypeTable(DShellExceptionArrayType, DShellExceptionArray.class);
		JavaMethodTable.Import(DShellExceptionArrayType, "[]", ZType.IntType, DShellExceptionArray.class, "GetIndex");
	}

	@Override public ZSourceEngine GetEngine() {
		return new ModifiedJavaEngine(new ModifiedTypeSafer(this), this);
	}

	@Override public void ImportLocalGrammar(ZNameSpace NameSpace) {
		super.ImportLocalGrammar(NameSpace);
		LibZen.ImportGrammar(NameSpace, DShellGrammar.class.getName());
	}

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
		else if(Node.Type.ShortName.equals("Task")) {
			this.invokeStaticMethod(Node.Type, ExecCommandTask);
		}
		else {
			this.invokeStaticMethod(Node.Type, ExecCommandVoid);
		}
	}

	public void VisitTryNode(DShellTryNode Node) {
		TryCatchLabel Label = new TryCatchLabel();
		this.TryCatchLabel.push(Label); // push
		// try block
		this.AsmBuilder.visitLabel(Label.beginTryLabel);
		Node.AST[DShellTryNode._Try].Accept(this);
		this.AsmBuilder.visitLabel(Label.endTryLabel);
		this.AsmBuilder.visitJumpInsn(GOTO, Label.finallyLabel);
		// catch block
		int size = Node.GetListSize();
		for(int i = 0; i < size; i++) {
			Node.GetListAt(i).Accept(this);
		}
		// finally block
		this.AsmBuilder.visitLabel(Label.finallyLabel);
		if(Node.AST[DShellTryNode._Finally] != null) {
			Node.AST[DShellTryNode._Finally].Accept(this);
		}
		this.TryCatchLabel.pop();
	}

	public void VisitCatchNode(DShellCatchNode Node) {
		Label catchLabel = new Label();
		TryCatchLabel Label = this.TryCatchLabel.peek();

		// prepare
		//TODO: add exception class name
		String throwType = this.AsmType(Node.ExceptionType).getInternalName();
		this.AsmBuilder.visitTryCatchBlock(Label.beginTryLabel, Label.endTryLabel, catchLabel, throwType);

		// catch block
		this.AsmBuilder.AddLocal(this.GetJavaClass(Node.ExceptionType), Node.ExceptionName);
		this.AsmBuilder.visitLabel(catchLabel);
		this.AsmBuilder.StoreLocal(Node.ExceptionName);
		Node.AST[ZCatchNode._Block].Accept(this);
		this.AsmBuilder.visitJumpInsn(GOTO, Label.finallyLabel);

		this.AsmBuilder.RemoveLocal(this.GetJavaClass(Node.ExceptionType), Node.ExceptionName);
	}

	public void VisitDummyNode(DShellDummyNode Node) {	// do nothing
	}

	@Override public void VisitThrowNode(ZThrowNode Node) {
		Node.AST[ZThrowNode._Expr].Accept(this);
		this.AsmBuilder.visitInsn(ATHROW);
	}

	@Override public void VisitInstanceOfNode(ZInstanceOfNode Node) {
		Class<?> JavaClass = this.GetJavaClass(Node.TargetType);
		if(Node.TargetType.IsIntType()) {
			JavaClass = Long.class;
		}
		else if(Node.TargetType.IsFloatType()) {
			JavaClass = Double.class;
		}
		else if(Node.TargetType.IsBooleanType()) {
			JavaClass = Boolean.class;
		}

		ZNode TargetNode = Node.AST[ZInstanceOfNode._Left];
		if(TargetNode.Type.IsIntType() || TargetNode.Type.IsFloatType() || TargetNode.Type.IsBooleanType()) {
			this.invokeBoxingMethod(TargetNode);
		}
		else {
			TargetNode.Accept(this);
		}
		this.AsmBuilder.visitTypeInsn(INSTANCEOF, JavaClass);
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

	private void loadJavaStaticMethod(Class<?> holderClass, String name, Class<?>... paramClasses) {
		try {
			ZFunc func = JavaCommonApi.ConvertToNativeFunc(holderClass.getMethod(name, paramClasses));
			this.SetDefinedFunc(func);
		}
		catch(Exception e) {
			Utils.fatal(1, "load static method faild: " + e.getMessage());
		}
	}
}
