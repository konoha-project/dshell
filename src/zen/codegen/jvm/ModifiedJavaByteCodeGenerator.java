package zen.codegen.jvm;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DUP;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.objectweb.asm.Type;

import dshell.ast.DShellCommandNode;
import dshell.lang.ModifiedTypeInfer;
import dshell.lib.TaskBuilder;

import zen.ast.ZNode;
import zen.codegen.jvm.JavaByteCodeGenerator;

public class ModifiedJavaByteCodeGenerator extends JavaByteCodeGenerator {
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
		this.TypeChecker = new ModifiedTypeInfer(this.Logger);
	}

	public void VisitCommandNode(DShellCommandNode Node) {
		ArrayList<ArrayList<ZNode>> Args = new ArrayList<ArrayList<ZNode>>();
		DShellCommandNode node = Node;
		while(node != null) {
			Args.add(node.ArgumentList);
			node = (DShellCommandNode) node.PipedNextNode;
		}
		// new String[][n]
		this.CurrentBuilder.AsmVisitor.visitLdcInsn(Args.size());
		this.CurrentBuilder.AsmVisitor.visitTypeInsn(ANEWARRAY, Type.getInternalName(String[].class));
		for(int i = 0; i < Args.size(); i++) {
			// new String[m];
			ArrayList<ZNode> Arg = Args.get(i);
			this.CurrentBuilder.AsmVisitor.visitInsn(DUP);
			this.CurrentBuilder.AsmVisitor.visitLdcInsn(i);
			this.CurrentBuilder.AsmVisitor.visitLdcInsn(Arg.size());
			this.CurrentBuilder.AsmVisitor.visitTypeInsn(ANEWARRAY, Type.getInternalName(String.class));
			for(int j = 0; j < Arg.size(); j++) {
				this.CurrentBuilder.AsmVisitor.visitInsn(DUP);
				this.CurrentBuilder.AsmVisitor.visitLdcInsn(j);
				Arg.get(j).Accept(this);
				this.CurrentBuilder.AsmVisitor.visitInsn(AASTORE);
			}
			this.CurrentBuilder.AsmVisitor.visitInsn(AASTORE);
		}
		if(Node.Type.IsBooleanType()) {
			this.CurrentBuilder.InvokeMethodCall(Node.Type, ExecCommandBool);
		}
		else if(Node.Type.IsIntType()) {
			this.CurrentBuilder.InvokeMethodCall(Node.Type, ExecCommandInt);
		}
		else if(Node.Type.IsStringType()) {
			this.CurrentBuilder.InvokeMethodCall(Node.Type, ExecCommandString);
		}
//		else if(LibZen.EqualsString(Node.Type.toString(), "Task")) {
//			this.CurrentBuilder.InvokeMethodCall(Node.Type, JLib.ExecCommandTask);
//		}
		else {
			this.CurrentBuilder.InvokeMethodCall(Node.Type, ExecCommandVoid);
		}
	}
}
