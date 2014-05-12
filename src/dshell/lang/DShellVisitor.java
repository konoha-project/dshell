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
	public void visitCommandNode(CommandNode node);
	public void visitTryNode(DShellTryNode node);
	public void visitCatchNode(DShellCatchNode node);
	public void visitContinueNode(BunContinueNode node);
	public void visitForNode(DShellForNode node);
	public void visitWrapperNode(DShellWrapperNode node);
	public void visitMatchRegexNode(MatchRegexNode node);
	public void visitInternalFuncCallNode(InternalFuncCallNode node);
}
