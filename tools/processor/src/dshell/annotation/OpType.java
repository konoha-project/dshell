package dshell.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OpType {
	OpName value() default OpName.DUMMY;

	public static enum OpName {
		/**
		 * dummy 
		 */
		DUMMY  (""),
		NOT    ("!"),
		BIT_NOT("~"),
		ADD    ("+"),
		SUB    ("-"),
		MUL    ("*"),
		DIV    ("/"),
		MOD    ("%"),
		LT     ("<"),
		GT     (">"),
		LE     ("<="),
		GE     (">="),
		EQ     ("=="),
		NE     ("!="),
		AND    ("&"),
		OR     ("|"),
		XOR    ("^"),
		REGEX_MATCH ("=~"),
		REGEX_UNMATCH ("!~"),
		ASSERT ("assert"),
		PRINT  ("printValue"),
		GETENV ("getEnv"),
		SETENV ("setEnv");

		private final String opSymbol;
		private OpName(String opSymbol) {
			this.opSymbol = opSymbol;
		}
		public String getOpSymbol() {
			return this.opSymbol;
		}
	}
}
