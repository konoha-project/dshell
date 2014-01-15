package dshell.lang;

import dshell.ast.DShellCommandNode;
import zen.ast.ZenNode;
import zen.lang.ZenDynamicTypeChecker;
import zen.lang.ZenSystem;
import zen.lang.ZenTypeChecker;
import zen.parser.ZenLogger;
import zen.parser.ZenNameSpace;

public class ModifiedDynamicTypeChecker extends ZenDynamicTypeChecker {
	public ModifiedDynamicTypeChecker(ZenLogger Logger) {
		super(Logger);
	}

	public void VisitCommandNode(DShellCommandNode Node) {	//TODO: support context type
		ZenNameSpace NameSpace = this.GetNameSpace();
		this.TypedNode(Node, ZenSystem.VoidType);
		Node.OptionNode = this.TypeCheck(Node.OptionNode, NameSpace, ZenSystem.IntType, ZenTypeChecker.DefaultTypeCheckPolicy);
		int size = Node.ArgumentList.size();
		for(int i = 0; i < size; i++) {
			ZenNode SubNode = Node.ArgumentList.get(i);
			SubNode = this.TypeCheck(SubNode, NameSpace, ZenSystem.StringType, ZenTypeChecker.DefaultTypeCheckPolicy);
			Node.ArgumentList.set(i, SubNode);
		}
		if(Node.PipedNextNode != null) {
			Node.ParentNode = this.TypeCheck(Node.PipedNextNode
					, NameSpace, ZenSystem.VoidType, DefaultTypeCheckPolicy);
		}
	}
}
