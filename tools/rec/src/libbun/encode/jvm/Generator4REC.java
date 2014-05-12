package libbun.encode.jvm;

import java.lang.reflect.Method;

import dshell.ast.sugar.DShellAssertNode;
import dshell.lib.Utils;
import dshell.lib.Utils.AssertionError;
import libbun.ast.SyntaxSugarNode;
import libbun.encode.jvm.DShellByteCodeGenerator;

public class Generator4REC extends DShellByteCodeGenerator {
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
		this.AsmBuilder.SetLineNumber(node);
		node.AST[DShellAssertNode._Expr].Accept(this);
		this.AsmBuilder.PushConst("");
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
