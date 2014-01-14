package dshell.codegen;

import dshell.ast.DShellCommandNode;
import zen.ast.ZenIntNode;
import zen.codegen.javascript.JavaScriptSourceGenerator;

public class ModifiedJavaScriptSourceGenerator extends JavaScriptSourceGenerator {
	public void VisitCommandNode(DShellCommandNode Node) {
		System.out.println("====  CommandNode =====");
		System.out.println(Node.Type);
		int size = Node.ArgumentList.size();
		for(int i = 0; i < size; i++) {
			if(i != 0) {
				System.out.print(" ");
			}
			System.out.print(Node.ArgumentList.get(i));
		}
		System.out.println();
		System.out.println("option: " + ((ZenIntNode)Node.OptionNode).Value);
		System.out.println("=======================");

		if(!Node.Type.IsVoidType()) {
			throw new RuntimeException("unsupported type: " + Node.Type);
		}

		this.CurrentBuilder.Append("(function() {\n");
		this.CurrentBuilder.Indent();
		this.CurrentBuilder.AppendIndent();
		this.CurrentBuilder.Append("var nativeClass = JavaImporter(Packages.dshell);\n");
		this.CurrentBuilder.AppendIndent();
		this.CurrentBuilder.Append("nativeClass.importClass(java.util.ArrayList);\n");
		this.CurrentBuilder.AppendIndent();
		this.CurrentBuilder.Append("nativeClass.importClass(Packages.dshell.TaskBuilder);\n");
		this.CurrentBuilder.AppendIndent();
		this.CurrentBuilder.Append("var argsList = new nativeClass.ArrayList();\n");
		DShellCommandNode currentNode = Node;
		int index = 0;
		while(currentNode != null) {
			String argList = "argList" + index++;
			this.CurrentBuilder.AppendIndent();
			this.CurrentBuilder.Append("var " + argList + " = new nativeClass.ArrayList();\n");
			int argSize = currentNode.ArgumentList.size();
			for(int i = 0; i < argSize; i++) {
				this.CurrentBuilder.AppendIndent();
				this.CurrentBuilder.Append(argList + ".add(");
				this.GenerateCode(currentNode.ArgumentList.get(i));
				this.CurrentBuilder.Append(");\n");
			}
			this.CurrentBuilder.AppendIndent();
			this.CurrentBuilder.Append("argsList.add(" + argList + ");\n");
			this.CurrentBuilder.Append("\n");
			currentNode = (DShellCommandNode) currentNode.PipedNextNode;
		}
		this.CurrentBuilder.AppendIndent();
		this.CurrentBuilder.Append("nativeClass.TaskBuilder.ExecCommandVoid(argsList);\n");
		this.CurrentBuilder.UnIndent();
		this.CurrentBuilder.Append("})()");
	}
}
