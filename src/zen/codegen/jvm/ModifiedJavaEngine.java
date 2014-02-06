package zen.codegen.jvm;

import java.util.ArrayList;

import zen.ast.ZNode;
import zen.codegen.jvm.JavaEngine;
import zen.codegen.jvm.JavaSolution;
import dshell.ast.DShellCommandNode;
import dshell.ast.DShellTryNode;
import dshell.lang.ModifiedTypeSafer;
import dshell.lib.TaskBuilder;

public class ModifiedJavaEngine extends JavaEngine {	//TODO: implement unsupported visit api
	public ModifiedJavaEngine(ModifiedTypeSafer TypeChecker, JavaSolution Generator) {
		super(TypeChecker, Generator);
	}

	public void VisitCommandNode(DShellCommandNode Node) {
		ArrayList<ArrayList<ZNode>> args = new ArrayList<ArrayList<ZNode>>();
		DShellCommandNode node = Node;
		while(node != null) {
			ArrayList<ZNode> argumentList = new ArrayList<ZNode>();
			int size = node.GetListSize();
			for(int i = 0; i < size; i++) {
				argumentList.add(node.GetListAt(i));
			}
			args.add(argumentList);
			node = (DShellCommandNode) node.PipedNextNode;
		}
		String[][] values = new String[args.size()][];
		for(int i = 0; i < values.length; i++) {
			ArrayList<ZNode> arg = args.get(i);
			int size = arg.size();
			values[i] = new String[size];
			for(int j = 0; j < size; j++) {
				values[i][j] = (String)this.Eval(arg.get(j));
			}
		}
		if(Node.Type.IsBooleanType()) {
			this.EvaledValue = TaskBuilder.ExecCommandBoolTopLevel(values);
		}
		else if(Node.Type.IsIntType()) {
			this.EvaledValue = TaskBuilder.ExecCommandIntTopLevel(values);
		}
		else if(Node.Type.IsStringType()) {
			this.EvaledValue = TaskBuilder.ExecCommandStringTopLevel(values);
		}
		else if(Node.Type.ShortName.equals("Task")) {
			this.EvaledValue = TaskBuilder.ExecCommandTaskTopLevel(values);
		}
		else {
			TaskBuilder.ExecCommandVoidTopLevel(values);
		}
	}

	public void VisitTryNode(DShellTryNode Node) {
		this.Unsupported(Node, "try");
	}
}
