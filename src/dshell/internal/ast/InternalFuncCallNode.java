package dshell.internal.ast;

import java.lang.reflect.Method;

import dshell.internal.lang.DShellVisitor;
import dshell.internal.lib.Utils;
import libbun.ast.BNode;
import libbun.encode.jvm.JavaTypeTable;
import libbun.parser.classic.LibBunVisitor;
import libbun.type.BType;

public class InternalFuncCallNode extends BNode {
	private final Method staticMethod;
	private final BType returnType;

	// call only void param
	public InternalFuncCallNode(BNode parentNode, Class<?> holderClass, String methodName) {
		super(parentNode, 0);
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
	public void Accept(LibBunVisitor visitor) {
		if(visitor instanceof DShellVisitor) {
			((DShellVisitor)visitor).visitInternalFuncCallNode(this);
		}
		else {
			Utils.fatal(1, visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
