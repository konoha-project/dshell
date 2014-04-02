package dshell.lang;

import libbun.lang.bun.shell.CommandNode;
import libbun.parser.sugar.ZContinueNode;
import dshell.ast.DShellCatchNode;
import dshell.ast.DShellForNode;
import dshell.ast.DShellTryNode;

public interface DShellVisitor {
	public void VisitCommandNode(CommandNode Node);
	public void VisitTryNode(DShellTryNode Node);
	public void VisitCatchNode(DShellCatchNode Node);
	public void VisitContinueNode(ZContinueNode Node);
	public void VisitForNode(DShellForNode Node);
}
