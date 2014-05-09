package dshell.ast;

import java.lang.reflect.Method;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BNode;
import libbun.encode.jvm.JavaTypeTable;
import libbun.parser.classic.LibBunVisitor;
import libbun.type.BType;

public class InternalFuncCallNode extends BNode {
	private final Method staticMethod;
	private final BType returnType;

	// call only void param
	public InternalFuncCallNode(BNode ParentNode, Class<?> holderClass, String methodName) {
		super(ParentNode, 0);
		try {
			this.staticMethod = holderClass.getMethod(methodName, new Class<?>[]{});
			this.returnType = JavaTypeTable.GetBunType(this.staticMethod.getReturnType());
		}
		catch(Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public BType getReturnType() {
		return this.returnType;
	}

	public Method getMethod() {
		return this.staticMethod;
	}

	@Override
	public void Accept(LibBunVisitor Visitor) {
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitInternalFuncCallNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
