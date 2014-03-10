package zen.codegen.jvm;

import java.util.ArrayList;

import zen.ast.ZInstanceOfNode;
import zen.ast.ZNode;
import zen.codegen.jvm.JavaEngine;
import zen.parser.ZLogger;
import dshell.ast.DShellCatchNode;
import dshell.ast.DShellCommandNode;
import dshell.ast.DShellDummyNode;
import dshell.ast.DShellTryNode;
import dshell.lang.DShellVisitor;
import dshell.lang.ModifiedTypeSafer;
import dshell.lib.Task;
import dshell.lib.TaskBuilder;
import dshell.remote.TaskArray;

public class ModifiedJavaEngine extends JavaEngine implements DShellVisitor {
	public ModifiedJavaEngine(ModifiedTypeSafer TypeChecker, ModifiedAsmGenerator Generator) {
		super(TypeChecker, Generator);
	}

	@Override
	public void VisitCommandNode(DShellCommandNode Node) {
		ArrayList<DShellCommandNode> nodeList = new ArrayList<DShellCommandNode>();
		DShellCommandNode node = Node;
		while(node != null) {
			nodeList.add(node);
			node = (DShellCommandNode) node.PipedNextNode;
		}
		int size = nodeList.size();
		String[][] values = new String[size][];
		for(int i = 0; i < size; i++) {
			DShellCommandNode currentNode = nodeList.get(i);
			int listSize = currentNode.GetListSize();
			values[i] = new String[listSize];
			for(int j = 0; j < listSize; j++) {
				values[i][j] = (String)this.Eval(currentNode.GetListAt(j));
			}
		}
		try {
			if(Node.Type.IsBooleanType()) {
				this.EvaledValue = TaskBuilder.ExecCommandBool(values);
			}
			else if(Node.Type.IsIntType()) {
				this.EvaledValue = TaskBuilder.ExecCommandInt(values);
			}
			else if(Node.Type.IsStringType()) {
				this.EvaledValue = TaskBuilder.ExecCommandString(values);
			}
			else if(this.Solution.GetJavaClass(Node.Type).equals(Task.class)) {
				this.EvaledValue = TaskBuilder.ExecCommandTask(values);
			}
			else if(this.Solution.GetJavaClass(Node.Type).equals(TaskArray.class)) {
				this.EvaledValue = TaskBuilder.ExecCommandTaskArray(values);
			}
			else {
				TaskBuilder.ExecCommandVoid(values);
			}
		}
		catch(Exception e) {
			ZLogger._LogError(Node.SourceToken, "invocation error: " + e);
			this.StopVisitor();
		}
	}

	@Override
	public void VisitTryNode(DShellTryNode Node) {
		this.Unsupported(Node);
	}

	@Override
	public void VisitCatchNode(DShellCatchNode Node) {
		this.Unsupported(Node);
	}

	@Override public void VisitInstanceOfNode(ZInstanceOfNode Node) {
		Class<?> JavaClass = this.Solution.GetJavaClass(Node.TargetType);
		if(Node.TargetType.IsIntType()) {
			JavaClass = Long.class;
		}
		else if(Node.TargetType.IsFloatType()) {
			JavaClass = Double.class;
		}
		else if(Node.TargetType.IsBooleanType()) {
			JavaClass = Boolean.class;
		}

		ZNode TargetNode = Node.LeftNode();
		Object Value = this.Eval(TargetNode);
		if(TargetNode.Type.IsIntType()) {
			Value = new Long((Long) Value);
		}
		else if(TargetNode.Type.IsFloatType()) {
			Value = new Double((Double) Value);
		}
		else if(TargetNode.Type.IsBooleanType()) {
			Value = new Boolean((Boolean) Value);
		}
		this.EvaledValue = Value.getClass().equals(JavaClass);
	}

	@Override
	public void VisitDummyNode(DShellDummyNode Node) {	// do nothing
	}
}
