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
	public void VisitSyntaxSugarNode(SyntaxSugarNode Node) {
		if(Node instanceof DShellAssertNode) {
			this.VisitAssertNode((DShellAssertNode) Node);
		}
		else {
			super.VisitSyntaxSugarNode(Node);
		}
	}

	private void VisitAssertNode(DShellAssertNode Node) {
		this.AsmBuilder.SetLineNumber(Node);
		Node.AST[DShellAssertNode._Expr].Accept(this);
		this.AsmBuilder.PushConst("");
		this.invokeStaticMethod(Node, this.assertREC);
	}

	public static void assertREC(boolean result, String location) {
		if(!result) {
			new AssertionError("").printStackTrace();
			System.err.println("REC assert 0 @" + location);
			System.exit(1);
		}
	}
}