package dshell.codegen.javascript;

import dshell.ast.DShellCommandNode;
import dshell.lang.ModifiedDynamicTypeChecker;
import zen.codegen.javascript.JavaScriptSourceGenerator;

public class ModifiedJavaScriptSourceGenerator extends JavaScriptSourceGenerator {
	public ModifiedJavaScriptSourceGenerator() {
		super();
		this.TypeChecker = new ModifiedDynamicTypeChecker(this.Logger);
	}

	public void VisitCommandNode(DShellCommandNode Node) {
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
		this.CurrentBuilder.AppendIndent();
		this.CurrentBuilder.Append("var options = new nativeClass.ArrayList();\n");
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
			this.CurrentBuilder.AppendIndent();
			this.CurrentBuilder.Append("options.add(");
			this.GenerateCode(currentNode.OptionNode);
			this.CurrentBuilder.Append(");\n");
			this.CurrentBuilder.Append("\n");
			currentNode = (DShellCommandNode) currentNode.PipedNextNode;
		}
		this.CurrentBuilder.AppendIndent();
		this.CurrentBuilder.Append("nativeClass.TaskBuilder.ExecCommandVoid(options, argsList);\n");
		this.CurrentBuilder.UnIndent();
		this.CurrentBuilder.Append("})()");
	}
}
