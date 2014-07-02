package dshell.lang;

import dshell.annotation.SharedClass;

@SharedClass
public class TypeCastException extends Exception {
	private static final long serialVersionUID = -338271634612816218L;

	TypeCastException(ClassCastException e) {
		super(e.getMessage());
		this.setStackTrace(this.recreateStackTrace(e.getStackTrace()));
	}
}
