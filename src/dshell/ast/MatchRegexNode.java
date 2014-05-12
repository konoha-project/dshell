package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BNode;
import libbun.ast.binary.ComparatorNode;
import libbun.lang.bun.BunPrecedence;
import libbun.parser.classic.LibBunVisitor;

public class MatchRegexNode extends ComparatorNode {
	private final boolean isUnmatch;

	public MatchRegexNode(BNode parentNode, boolean isUnmatch) {
		super(parentNode, BunPrecedence._CStyleCOMPARE);
		this.isUnmatch = isUnmatch;
	}

	@Override
	public String GetOperator() {
		return this.isUnmatch ? "!~" : "=~";
	}

	@Override
	public void Accept(LibBunVisitor visitor) {
		if(visitor instanceof DShellVisitor) {
			((DShellVisitor)visitor).visitMatchRegexNode(this);
		}
		else {
			Utils.fatal(1, visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
