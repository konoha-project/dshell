package zen.codegen.jvm;

import java.util.ArrayList;

import zen.ast.ZNode;
import dshell.ast.DShellCommandNode;
import dshell.lang.ModifiedTypeInfer;
import dshell.lib.TaskBuilder;

public class ModifiedReflectionEngine extends JavaReflectionEngine {	//TODO: implement unsupported visit api
	public ModifiedReflectionEngine(ModifiedTypeInfer TypeChecker, ModifiedJavaByteCodeGenerator Generator) {
		super(TypeChecker, Generator);
	}

	public void VisitCommandNode(DShellCommandNode Node) {
		ArrayList<ArrayList<ZNode>> args = new ArrayList<ArrayList<ZNode>>();
		DShellCommandNode node = Node;
		while(node != null) {
			args.add(node.ArgumentList);
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
			this.EvaledValue = TaskBuilder.ExecCommandBool(values);
		}
		else if(Node.Type.IsIntType()) {
			this.EvaledValue = TaskBuilder.ExecCommandInt(values);
		}
		else if(Node.Type.IsStringType()) {
			this.EvaledValue = TaskBuilder.ExecCommandString(values);
		}
		else {
			TaskBuilder.ExecCommandVoid(values);
		}
	}
}
