package zen.codegen.jvm;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.objectweb.asm.Type;

import dshell.ast.DShellCommandNode;
import dshell.ast.DShellTryNode;
import dshell.exception.DShellException;
import dshell.exception.MultipleException;
import dshell.exception.NullException;
import dshell.exception.UnimplementedErrnoException;
import dshell.lang.ModifiedTypeInfer;
import dshell.lib.ClassListLoader;
import dshell.lib.Task;
import dshell.lib.TaskBuilder;
import dshell.util.Utils;
import zen.ast.ZNode;
import zen.deps.NativeTypeTable;
import zen.lang.ZenEngine;
import zen.parser.ZToken;
import zen.type.ZType;

public class ModifiedJavaByteCodeGenerator extends Java6ByteCodeGenerator {
	private static Method ExecCommandVoid;
	private static Method ExecCommandBool;
	private static Method ExecCommandInt;
	private static Method ExecCommandString;
	private static Method ExecCommandTask;
	
	static {
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
		NativeMethodTable.Import(ZType.StringType, "=~", ZType.StringType, Utils.class, "matchRegex");
		NativeMethodTable.Import(ZType.StringType, "!~", ZType.StringType, Utils.class, "unmatchRegex");
		NativeMethodTable.Import("assert", ZType.BooleanType, Utils.class, "assertResult");
		NativeMethodTable.Import("log", ZType.VarType, Utils.class, "log");
	}

	public ModifiedJavaByteCodeGenerator() {
		super();
		this.importNativeClass(Task.class);
		this.importNativeClass(DShellException.class);
		this.importNativeClass(MultipleException.class);
		this.importNativeClass(UnimplementedErrnoException.class);
		this.importNativeClass(NullException.class);
		this.importNativeClassList(new ClassListLoader("dshell.exception.errno").loadClassList());
	}

	@Override public ZenEngine GetEngine() {
		return new ModifiedReflectionEngine(new ModifiedTypeInfer(this), this);
	}

	public void VisitCommandNode(DShellCommandNode Node) {
		ArrayList<ArrayList<ZNode>> Args = new ArrayList<ArrayList<ZNode>>();
		DShellCommandNode node = Node;
		while(node != null) {
			Args.add(node.ArgumentList);
			node = (DShellCommandNode) node.PipedNextNode;
		}
		// new String[][n]
		this.CurrentBuilder.visitLdcInsn(Args.size());
		this.CurrentBuilder.visitTypeInsn(ANEWARRAY, Type.getInternalName(String[].class));
		for(int i = 0; i < Args.size(); i++) {
			// new String[m];
			ArrayList<ZNode> Arg = Args.get(i);
			this.CurrentBuilder.visitInsn(DUP);
			this.CurrentBuilder.visitLdcInsn(i);
			this.CurrentBuilder.visitLdcInsn(Arg.size());
			this.CurrentBuilder.visitTypeInsn(ANEWARRAY, Type.getInternalName(String.class));
			for(int j = 0; j < Arg.size(); j++) {
				this.CurrentBuilder.visitInsn(DUP);
				this.CurrentBuilder.visitLdcInsn(j);
				Arg.get(j).Accept(this);
				this.CurrentBuilder.visitInsn(AASTORE);
			}
			this.CurrentBuilder.visitInsn(AASTORE);
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
		this.CurrentBuilder.visitLabel(Label.beginTryLabel);
		Node.TryNode.Accept(this);
		this.CurrentBuilder.visitLabel(Label.endTryLabel);
		this.CurrentBuilder.visitJumpInsn(GOTO, Label.finallyLabel);
		// catch block
		for(ZNode CatchNode : Node.CatchNodeList) {
			CatchNode.Accept(this);
		}
		// finally block
		this.CurrentBuilder.visitLabel(Label.finallyLabel);
		if(Node.FinallyNode != null) {
			Node.FinallyNode.Accept(this);
		}
		this.TryCatchLabel.pop();
	}

	private void invokeStaticMethod(ZType type, Method method) { //TODO: check return type cast
		String owner = Type.getInternalName(method.getDeclaringClass());
		this.CurrentBuilder.visitMethodInsn(INVOKESTATIC, owner, method.getName(), Type.getMethodDescriptor(method));
	}

	private void importNativeClass(Class<?> classObject) {
		ZType type = NativeTypeTable.GetZenType(classObject);
		this.RootNameSpace.SetTypeName(classObject.getSimpleName(), type, new ZToken(0, classObject.getCanonicalName(), 0));
	}

	private void importNativeClassList(ArrayList<Class<?>> classObjList) {
		for(Class<?> classObj : classObjList) {
			this.importNativeClass(classObj);
		}
	}
}
