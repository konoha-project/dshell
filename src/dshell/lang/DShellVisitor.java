package dshell.lang;

import zen.ast.sugar.ZContinueNode;
import dshell.ast.DShellCatchNode;
import dshell.ast.DShellCommandNode;
import dshell.ast.DShellDummyNode;
import dshell.ast.DShellForNode;
import dshell.ast.DShellTryNode;

public interface DShellVisitor {
	public void VisitCommandNode(DShellCommandNode Node);
	public void VisitTryNode(DShellTryNode Node);
	public void VisitCatchNode(DShellCatchNode Node);
	public void VisitDummyNode(DShellDummyNode Node);
	public void VisitContinueNode(ZContinueNode Node);
	public void VisitForNode(DShellForNode Node);
}
