package dshell.ast;

import dshell.lang.DShellVisitor;
import dshell.lib.Utils;
import libbun.ast.BNode;
import libbun.ast.binary.ComparatorNode;
import libbun.lang.bun.BunPrecedence;
import libbun.parser.LibBunVisitor;

public class MatchRegexNode extends ComparatorNode {
	private final boolean IsUnmatch;

	public MatchRegexNode(BNode ParentNode, boolean IsUnmatch) {
		super(ParentNode, BunPrecedence._CStyleCOMPARE);
		this.IsUnmatch = IsUnmatch;
	}

	@Override
	public String GetOperator() {
		return this.IsUnmatch ? "!~" : "=~";
	}

	@Override
	public void Accept(LibBunVisitor Visitor) {
		if(Visitor instanceof DShellVisitor) {
			((DShellVisitor)Visitor).VisitMatchRegxNode(this);
		}
		else {
			Utils.fatal(1, Visitor.getClass().getName() + " is unsupported Visitor");
		}
	}
}
