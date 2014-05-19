package dshell.internal.jvm;

import org.objectweb.asm.Label;

class TryCatchLabel {
	public Label BeginTryLabel;
	public Label EndTryLabel;
	public Label FinallyLabel;
	public TryCatchLabel() {
		this.BeginTryLabel = new Label();
		this.EndTryLabel = new Label();
		this.FinallyLabel = new Label();
	}
}