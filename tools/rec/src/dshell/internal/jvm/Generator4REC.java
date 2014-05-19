package dshell.internal.jvm;

import java.lang.reflect.Method;

import dshell.internal.ast.sugar.DShellAssertNode;
import dshell.internal.lib.Utils;
import dshell.internal.lib.Utils.AssertionError;
import libbun.ast.SyntaxSugarNode;

public class Generator4REC extends JavaByteCodeGenerator {
	private Method assertREC;

	public Generator4REC() {
		super();
		try {
			this.assertREC = this.getClass().getMethod("assertREC", boolean.class, String.class);
		}
		catch(Throwable e) {
			e.printStackTrace();
			Utils.fatal(1, "method loading failed");
		}
	}
	@Override
	public void VisitSyntaxSugarNode(SyntaxSugarNode node) {
		if(node instanceof DShellAssertNode) {
			this.VisitAssertNode((DShellAssertNode) node);
		}
		else {
			super.VisitSyntaxSugarNode(node);
		}
	}

	private void VisitAssertNode(DShellAssertNode node) {
		this.asmBuilder.setLineNumber(node);
		node.AST[DShellAssertNode._Expr].Accept(this);
		this.asmBuilder.pushConst("");
		this.invokeStaticMethod(node, this.assertREC);
	}

	public static void assertREC(boolean result, String location) {
		if(!result) {
			new AssertionError("").printStackTrace();
			System.err.println("REC assert 0 @" + location);
			System.exit(1);
		}
	}
}
