package dshell.lang;

import dshell.ast.DShellCatchNode;
import dshell.ast.DShellCommandNode;
import dshell.ast.DShellDummyNode;
import dshell.ast.DShellTryNode;

public interface DShellVisitor {
	public void VisitCommandNode(DShellCommandNode Node);
	public void VisitTryNode(DShellTryNode Node);
	public void VisitCatchNode(DShellCatchNode Node);
	public void VisitDummyNode(DShellDummyNode Node);
}
