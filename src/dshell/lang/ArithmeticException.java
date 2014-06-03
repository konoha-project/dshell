package dshell.lang;

import dshell.annotation.Shared;
import dshell.annotation.SharedClass;

/**
 * represent arithmetic exception such a zero division
 * @author skgchxngsxyz-osx
 *
 */
@SharedClass("Exception")
public class ArithmeticException extends Exception {
	private static final long serialVersionUID = 874238588805165055L;

	@Shared
	public ArithmeticException() {
		super();
	}

	@Shared
	public ArithmeticException(String message) {
		super(message);
	}

	public static void throwIfZeroDiv(long right) {
		if(right == 0) {
			throw new ArithmeticException("/ by zero");
		}
	}

	public static void throwIfZeroDiv(double right) {
		if(right == 0) {
			throw new ArithmeticException("/ by zero");
		}
	}

	public static void throwIfZeroMod(long right) {
		if(right == 0) {
			throw new ArithmeticException("% by zero");
		}
	}

	public static void throwIfZeroMod(double right) {
		if(right == 0) {
			throw new ArithmeticException("% by zero");
		}
	}
}
