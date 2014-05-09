package dshell.lang;

import libbun.ast.sugar.BunContinueNode;
import dshell.ast.CommandNode;
import dshell.ast.DShellCatchNode;
import dshell.ast.DShellForNode;
import dshell.ast.DShellTryNode;
import dshell.ast.DShellWrapperNode;
import dshell.ast.InternalFuncCallNode;
import dshell.ast.MatchRegexNode;

public interface DShellVisitor {
	public void VisitCommandNode(CommandNode Node);
	public void VisitTryNode(DShellTryNode Node);
	public void VisitCatchNode(DShellCatchNode Node);
	public void VisitContinueNode(BunContinueNode Node);
	public void VisitForNode(DShellForNode Node);
	public void VisitWrapperNode(DShellWrapperNode Node);
	public void VisitMatchRegexNode(MatchRegexNode Node);
	public void VisitInternalFuncCallNode(InternalFuncCallNode Node);
}
