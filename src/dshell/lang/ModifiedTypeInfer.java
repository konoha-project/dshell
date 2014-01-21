package dshell.lang;

import dshell.ast.DShellCommandNode;
import zen.ast.ZenNode;
import zen.lang.ZenSystem;
import zen.lang.ZenType;
import zen.lang.ZenTypeChecker;
import zen.lang.ZenTypeInfer;
import zen.parser.ZLogger;
import zen.parser.ZNameSpace;

public class ModifiedTypeInfer extends ZenTypeInfer {
	public ModifiedTypeInfer(ZLogger Logger) {
		super(Logger);
	}

	public void VisitCommandNode(DShellCommandNode Node) {	//TODO: support context type
		ZNameSpace NameSpace = this.GetNameSpace();
		ZenType ContextType = this.GetContextType();
		int size = Node.ArgumentList.size();
		for(int i = 0; i < size; i++) {
			ZenNode SubNode = Node.ArgumentList.get(i);
			SubNode = this.TypeCheck(SubNode, NameSpace, ZenSystem.StringType, ZenTypeChecker.DefaultTypeCheckPolicy);
			Node.ArgumentList.set(i, SubNode);
		}
		if(Node.PipedNextNode != null) {
			Node.ParentNode = this.TypeCheck(Node.PipedNextNode, NameSpace, ContextType, ZenTypeChecker.DefaultTypeCheckPolicy);
		}
		this.TypedNode(Node, ContextType);
	}
}
