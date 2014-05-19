package dshell.internal.jvm;

import libbun.ast.BNode;
import libbun.ast.LocalDefinedNode;
import libbun.type.BType;

public class JavaStaticFieldNode extends LocalDefinedNode {
	Class<?> StaticClass;
	String FieldName;
	JavaStaticFieldNode(BNode ParentNode, Class<?> StaticClass, BType FieldType, String FieldName) {
		super(ParentNode, 0);
		this.StaticClass = StaticClass;
		this.Type = FieldType;
		this.FieldName = FieldName;
	}
}
