package dshell.lang;

import libbun.ast.sugar.BunContinueNode;
import libbun.lang.bun.shell.CommandNode;
import dshell.ast.DShellCatchNode;
import dshell.ast.DShellForNode;
import dshell.ast.DShellTryNode;

public interface DShellVisitor {
	public void VisitCommandNode(CommandNode Node);
	public void VisitTryNode(DShellTryNode Node);
	public void VisitCatchNode(DShellCatchNode Node);
	public void VisitContinueNode(BunContinueNode Node);
	public void VisitForNode(DShellForNode Node);
}
