package dshell.internal.lang;

import libbun.ast.sugar.BunContinueNode;
import dshell.internal.ast.CommandNode;
import dshell.internal.ast.DShellCatchNode;
import dshell.internal.ast.DShellForNode;
import dshell.internal.ast.DShellTryNode;
import dshell.internal.ast.DShellWrapperNode;
import dshell.internal.ast.InternalFuncCallNode;
import dshell.internal.ast.MatchRegexNode;

public interface DShellVisitor {
	public void visitCommandNode(CommandNode node);
	public void visitTryNode(DShellTryNode node);
	public void visitCatchNode(DShellCatchNode node);
	public void visitContinueNode(BunContinueNode node);
	public void visitForNode(DShellForNode node);
	public void visitWrapperNode(DShellWrapperNode node);
	public void visitMatchRegexNode(MatchRegexNode node);
	public void visitInternalFuncCallNode(InternalFuncCallNode node);
}
