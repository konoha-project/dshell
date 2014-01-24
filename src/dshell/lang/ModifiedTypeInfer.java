package dshell.lang;

import dshell.ast.DShellCommandNode;
import zen.ast.ZNode;
import zen.lang.ZSystem;
import zen.lang.ZType;
import zen.lang.ZenTypeChecker;
import zen.lang.ZenTypeInfer;
import zen.parser.ZLogger;
import zen.parser.ZNameSpace;

public class ModifiedTypeInfer extends ZenTypeInfer {
	public ModifiedTypeInfer(ZLogger Logger) {
		super(Logger);
	}

	public void VisitCommandNode(DShellCommandNode Node) {
		ZNameSpace NameSpace = this.GetNameSpace();
		ZType ContextType = this.GetContextType();
		if(!ContextType.IsBooleanType() && !ContextType.IsIntType() && !ContextType.IsStringType() && !ContextType.IsVoidType()) {
			ContextType = ZSystem.VoidType;
		}
		int size = Node.ArgumentList.size();
		for(int i = 0; i < size; i++) {
			ZNode SubNode = Node.ArgumentList.get(i);
			SubNode = this.TypeCheck(SubNode, NameSpace, ZSystem.StringType, ZenTypeChecker.DefaultTypeCheckPolicy);
			Node.ArgumentList.set(i, SubNode);
		}
		if(Node.PipedNextNode != null) {
			Node.ParentNode = this.TypeCheck(Node.PipedNextNode, NameSpace, ContextType, ZenTypeChecker.DefaultTypeCheckPolicy);
		}
		this.TypedNode(Node, ContextType);
	}
}
