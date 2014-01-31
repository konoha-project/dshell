package dshell.lang;

import zen.ast.ZNode;
import zen.deps.NativeTypeTable;
import zen.lang.ZenTypeSafer;
import zen.parser.ZLogger;
import zen.parser.ZNameSpace;
import zen.type.ZType;
import dshell.ast.DShellCommandNode;
import dshell.lib.Task;

public class ModifiedTypeInfer extends ZenTypeSafer {
	public ModifiedTypeInfer(ZLogger Logger) {
		super(Logger);
	}

	public void VisitCommandNode(DShellCommandNode Node) {
		ZNameSpace NameSpace = this.GetNameSpace();
		ZType ContextType = this.GetContextType();
		if(!ContextType.IsBooleanType() && !ContextType.IsIntType() && !ContextType.IsStringType() && !ContextType.IsVoidType()) {
			ContextType = NativeTypeTable.GetZenType(Task.class);
		}
		int size = Node.ArgumentList.size();
		for(int i = 0; i < size; i++) {
			ZNode SubNode = Node.ArgumentList.get(i);
			SubNode = this.CheckType(SubNode, NameSpace, ZType.StringType);
			Node.ArgumentList.set(i, SubNode);
		}
		if(Node.PipedNextNode != null) {
			Node.ParentNode = this.CheckType(Node.PipedNextNode, NameSpace, ContextType);
		}
		this.TypedNode(Node, ContextType);
	}
}
