package zen.codegen.jvm;

import java.util.ArrayList;

import zen.ast.ZNode;
import dshell.ast.DShellCommandNode;
import dshell.lib.TaskBuilder;

public class ModifiedTopLevelInterpreter extends TopLevelInterpreter {	//TODO: implement unsupported visit api
	public ModifiedTopLevelInterpreter(ModifiedJavaByteCodeGenerator Generator) {
		super(Generator);
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
			TaskBuilder.ExecCommandBool(values);
		}
		else if(Node.Type.IsIntType()) {
			TaskBuilder.ExecCommandInt(values);
		}
		else if(Node.Type.IsStringType()) {
			TaskBuilder.ExecCommandString(values);
		}
		else {
			TaskBuilder.ExecCommandVoid(values);
		}
	}
}
