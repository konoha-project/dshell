package dshell.internal.lib;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import dshell.annotation.OpHolder;
import dshell.annotation.OpType;
import dshell.annotation.Shared;
import dshell.annotation.OpType.OpName;
import dshell.lang.ArithmeticException;

/**
 * D-Shell basic operator definition.
 * @author skgchxngsxyz-osx
 *
 */
@OpHolder
public class Operator {
	// unary op
	// PLUS
	@Shared @OpType(OpName.ADD) public static long   plus(long right)   { return +right; }
	@Shared @OpType(OpName.ADD) public static double plus(double right) { return +right; }

	// MINUS
	@Shared @OpType(OpName.SUB) public static long   minus(long right)   { return -right; }
	@Shared @OpType(OpName.SUB) public static double minus(double right) { return -right; }

	// NOT
	@Shared @OpType(OpName.NOT) public static boolean not(boolean right) { return !right; }

	// BIT_NOT
	@Shared @OpType(OpName.BIT_NOT) public static long bitnot(long right) { return ~right; }

	// binary op
	// ADD
	@Shared @OpType(OpName.ADD) public static long   add(long left, long right)      { return left + right; }
	@Shared @OpType(OpName.ADD) public static double add(long left, double right)    { return left + right; }
	@Shared @OpType(OpName.ADD) public static double add(double left, long right)    { return left + right; }
	@Shared @OpType(OpName.ADD) public static double add(double left, double right)  { return left + right; }
	// string concat
	@Shared @OpType(OpName.ADD) public static String add(String left, long right)    { return left + right; }
	@Shared @OpType(OpName.ADD) public static String add(String left, double right)  { return left + right; }
	@Shared @OpType(OpName.ADD) public static String add(String left, boolean right) { return left + right; }
	@Shared @OpType(OpName.ADD) public static String add(String left, Object right)  { return left + right; }
	@Shared @OpType(OpName.ADD) public static String add(long left, String right)    { return left + right; }
	@Shared @OpType(OpName.ADD) public static String add(double left, String right)  { return left + right; }
	@Shared @OpType(OpName.ADD) public static String add(boolean left, String right) { return left + right; }
	@Shared @OpType(OpName.ADD) public static String add(Object left, String right)  { return left + right; }

	// SUB
	@Shared @OpType(OpName.SUB) public static long   sub(long left, long right)     { return left - right; }
	@Shared @OpType(OpName.SUB) public static double sub(long left, double right)   { return left - right; }
	@Shared @OpType(OpName.SUB) public static double sub(double left, long right)   { return left - right; }
	@Shared @OpType(OpName.SUB) public static double sub(double left, double right) { return left - right; }

	// MUL
	@Shared @OpType(OpName.MUL) public static long   mul(long left, long right)     { return left * right; }
	@Shared @OpType(OpName.MUL) public static double mul(long left, double right)   { return left * right; }
	@Shared @OpType(OpName.MUL) public static double mul(double left, long right)   { return left * right; }
	@Shared @OpType(OpName.MUL) public static double mul(double left, double right) { return left * right; }

	//DIV
	@Shared @OpType(OpName.DIV) public static long   div(long left, long right) {
		ArithmeticException.throwIfZeroDiv(right); return left / right;
	}
	@Shared @OpType(OpName.DIV) public static double div(long left, double right) {
		ArithmeticException.throwIfZeroDiv(right); return left / right;
	}
	@Shared @OpType(OpName.DIV) public static double div(double left, long right) {
		ArithmeticException.throwIfZeroDiv(right); return left / right;
	}
	@Shared @OpType(OpName.DIV) public static double div(double left, double right) {
		ArithmeticException.throwIfZeroDiv(right); return left / right;
	}

	// MOD
	@Shared @OpType(OpName.MOD) public static long   mod(long left, long right) {
		ArithmeticException.throwIfZeroMod(right); return left % right;
	}
	@Shared @OpType(OpName.MOD) public static double mod(long left, double right) {
		ArithmeticException.throwIfZeroDiv(right); return left % right;
	}
	@Shared @OpType(OpName.MOD) public static double mod(double left, long right) {
		ArithmeticException.throwIfZeroDiv(right); return left % right;
	}
	@Shared @OpType(OpName.MOD) public static double mod(double left, double right) {
		ArithmeticException.throwIfZeroDiv(right); return left % right;
	}

	// LT
	@Shared @OpType(OpName.LT) public static boolean lessThan(long left, long right)     { return left < right; }
	@Shared @OpType(OpName.LT) public static boolean lessThan(long left, double right)   { return left < right; }
	@Shared @OpType(OpName.LT) public static boolean lessThan(double left, long right)   { return left < right; }
	@Shared @OpType(OpName.LT) public static boolean lessThan(double left, double right) { return left < right; }

	// GT
	@Shared @OpType(OpName.GT) public static boolean greaterThan(long left, long right)     { return left > right; }
	@Shared @OpType(OpName.GT) public static boolean greaterThan(long left, double right)   { return left > right; }
	@Shared @OpType(OpName.GT) public static boolean greaterThan(double left, long right)   { return left > right; }
	@Shared @OpType(OpName.GT) public static boolean greaterThan(double left, double right) { return left > right; }

	// LE
	@Shared @OpType(OpName.LE) public static boolean lessEqualsThan(long left, long right)     { return left <= right; }
	@Shared @OpType(OpName.LE) public static boolean lessEqualsThan(long left, double right)   { return left <= right; }
	@Shared @OpType(OpName.LE) public static boolean lessEqualsThan(double left, long right)   { return left <= right; }
	@Shared @OpType(OpName.LE) public static boolean lessEqualsThan(double left, double right) { return left <= right; }

	// GE
	@Shared @OpType(OpName.GE) public static boolean greaterEqualsThan(long left, long right)     { return left >= right; }
	@Shared @OpType(OpName.GE) public static boolean greaterEqualsThan(long left, double right)   { return left >= right; }
	@Shared @OpType(OpName.GE) public static boolean greaterEqualsThan(double left, long right)   { return left >= right; }
	@Shared @OpType(OpName.GE) public static boolean greaterEqualsThan(double left, double right) { return left >= right; }

	// EQ
	@Shared @OpType(OpName.EQ) public static boolean equals(long left, long right)       { return left == right; }
	@Shared @OpType(OpName.EQ) public static boolean equals(long left, double right)     { return left == right; }
	@Shared @OpType(OpName.EQ) public static boolean equals(double left, long right)     { return left == right; }
	@Shared @OpType(OpName.EQ) public static boolean equals(double left, double right)   { return left == right; }

	@Shared @OpType(OpName.EQ) public static boolean equals(boolean left, boolean right) { return left == right; }
	@Shared @OpType(OpName.EQ) public static boolean equals(String left, String right)   { return left.equals(right); }
	@Shared @OpType(OpName.EQ) public static boolean equals(Object left, Object right)   { return left.equals(right); }

	// NE
	@Shared @OpType(OpName.NE) public static boolean notEquals(long left, long right)       { return left != right; }
	@Shared @OpType(OpName.NE) public static boolean notEquals(long left, double right)     { return left != right; }
	@Shared @OpType(OpName.NE) public static boolean notEquals(double left, long right)     { return left != right; }
	@Shared @OpType(OpName.NE) public static boolean notEquals(double left, double right)   { return left != right; }

	@Shared @OpType(OpName.NE) public static boolean notEquals(boolean left, boolean right) { return left != right; }
	@Shared @OpType(OpName.NE) public static boolean notEquals(String left, String right)   { return !left.equals(right); }
	@Shared @OpType(OpName.NE) public static boolean notEquals(Object left, Object right)   { return !left.equals(right); }

	// AND
	@Shared @OpType(OpName.AND) public static long and(long left, long right) { return left & right; }

	// OR
	@Shared @OpType(OpName.OR) public static long or(long left, long right) { return left | right; }

	// XOR
	@Shared @OpType(OpName.XOR) public static long xor(long left, long right) { return left ^ right; }

	// regex match
	@Shared @OpType(OpName.REGEX_MATCH) public static boolean matchRegex(String target, String regex) {
		try {
			Pattern pattern = Pattern.compile(regex);
			return pattern.matcher(target).find();
		}
		catch (PatternSyntaxException e) {
		}
		return false;
	}

	@Shared @OpType(OpName.REGEX_UNMATCH) public static boolean unmatchRegex(String target, String regex) {
		return !matchRegex(target, regex);
	}
	
	
	// additional operator
	// ASSERT
	@Shared @OpType(OpName.ASSERT) public static void assertDShell(boolean result) {
		if(!result) {
			new AssertionError("").printStackTrace();
			System.exit(1);
		}
	}

	private static class AssertionError extends dshell.lang.Exception {
		private static final long serialVersionUID = 5837757502752361730L;

		public AssertionError(String message) {
			super(message);
		}
	}

	// PRINT
	@Shared @OpType(OpName.PRINT) public static void printValue(Object value, String typeName) {
		System.out.println("(" + typeName + ") " + value);
	}

	@Shared @OpType(OpName.GETENV) public static String getEnv(String key) {
		String env = RuntimeContext.getInstance().getenv(key);
		return env == null ? "" : env;
	}

	@Shared @OpType(OpName.SETENV) public static String setEnv(String key, String env) {
		int ret = RuntimeContext.getInstance().setenv(key, env, true);
		return ret == 0 ? env : "";
	}
}
