package zen.codegen.jvm;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.objectweb.asm.Type;

import dshell.ast.DShellCommandNode;
import dshell.lang.ModifiedTypeInfer;
import dshell.lib.TaskBuilder;
import zen.ast.ZNode;
import zen.lang.ZType;
import zen.lang.ZenEngine;

public class ModifiedJavaByteCodeGenerator extends Java6ByteCodeGenerator {
	private static Method ExecCommandVoid;
	private static Method ExecCommandBool;
	private static Method ExecCommandInt;
	private static Method ExecCommandString;
	//private static Method ExecCommandTask;
	
	static {
		try {
			ExecCommandVoid = TaskBuilder.class.getMethod("ExecCommandVoid", String[][].class);
			ExecCommandBool = TaskBuilder.class.getMethod("ExecCommandBool", String[][].class);
			ExecCommandInt = TaskBuilder.class.getMethod("ExecCommandInt", String[][].class);
			ExecCommandString = TaskBuilder.class.getMethod("ExecCommandString", String[][].class);
			//ExecCommandTask = TaskBuilder.class.getMethod("ExecCommandTask", String[][].class);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.err.println("method loading failed");
			System.exit(1);
		}
	}

	public ModifiedJavaByteCodeGenerator() {
		super();
	}

	@Override public ZenEngine GetEngine() {
		return new ModifiedReflectionEngine(new ModifiedTypeInfer(this.Logger), this);
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
//		else if(LibZen.EqualsString(Node.Type.toString(), "Task")) {
//			this.CurrentBuilder.InvokeMethodCall(Node.Type, JLib.ExecCommandTask);
//		}
		else {
			this.invokeStaticMethod(Node.Type, ExecCommandVoid);
		}
	}

	private void invokeStaticMethod(ZType type, Method method) { //TODO: check return type cast
		String owner = Type.getInternalName(method.getDeclaringClass());
		this.CurrentBuilder.visitMethodInsn(INVOKESTATIC, owner, method.getName(), Type.getMethodDescriptor(method));
	}
}
