package dshell.internal.parser.error;

import dshell.internal.parser.Node;

public class DShellErrorListener {
	public static enum TypeErrorKind {
		Unresolved     ("having unresolved type"),
		Required        ("require %s, but is %s"),
		DefinedSymbol  ("already defined symbol: %s"),
		InsideLoop     ("only available inside loop statement"),
		UnfoundReturn  ("not found return statement"),
		UndefinedSymbol("undefined symbol: %s"),
		UndefinedField ("undefined field: %s"),
		CastOp         ("unsupported cast op: %s -> %s"),
		UnaryOp        ("undefined operator: %s %s"),
		BinaryOp       ("undefined operator: %s %s %s"),
		UnmatchParam   ("not match parameter, require size is %d, but is %d"),
		UndefinedMethod("undefined method: %s"),
		UndefinedInit  ("undefined constructor: %s"),
		Unreachable    ("found unreachable code"),
		InsideFunc     ("only available inside function"),
		NotNeedExpr    ("not need expression"),
		Assignable     ("require assignable node"),
		ReadOnly       ("read only value"),
		
		Unimplemented  ("unimplemented type checker api: %s");

		private final String template;

		private TypeErrorKind(String template) {
			this.template = template;
		}

		public String getTemplate() {
			return this.template;
		}
	}

	public void reportTypeError(Node node, TypeErrorKind kind, Object... args) {
		throw new TypeCheckException(node.getToken(), String.format(kind.getTemplate(), args));
	}
}
