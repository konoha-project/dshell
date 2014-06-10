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
		DUMMY { @Override public String getOpSymbol() { return ""; } },

		ADD { @Override public String getOpSymbol() { return "+"; } },
		SUB { @Override public String getOpSymbol() { return "-"; } },
		MUL { @Override public String getOpSymbol() { return "*"; } },
		DIV { @Override public String getOpSymbol() { return "/"; } },
		MOD { @Override public String getOpSymbol() { return "%"; } },
		LT  { @Override public String getOpSymbol() { return "<"; } },
		GT  { @Override public String getOpSymbol() { return ">"; } },
		LE  { @Override public String getOpSymbol() { return "<="; } },
		GE  { @Override public String getOpSymbol() { return ">="; } },
		EQ  { @Override public String getOpSymbol() { return "=="; } },
		NE  { @Override public String getOpSymbol() { return "!="; } },
		AND { @Override public String getOpSymbol() { return "&"; } },
		OR  { @Override public String getOpSymbol() { return "|"; } },
		XOR { @Override public String getOpSymbol() { return "^"; } },
		ASSERT { @Override public String getOpSymbol() { return "assert"; } },
		PRINT { @Override public String getOpSymbol() { return "printValue"; } },
		GETENV { @Override public String getOpSymbol() { return "getEnv"; } },
		SETENV { @Override public String getOpSymbol() { return "setEnv"; } };

		/**
		 * must override
		 * @return
		 */
		public String getOpSymbol() {
			return "";
		}
	}
}
