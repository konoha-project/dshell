package dshell.internal.exe;

import java.util.EnumSet;

/**
 * definition of ExecutionEngine.
 * if you vreate your own engine, you must implement it.
 * @author skgchxngsxyz-osx
 *
 */
public interface ExecutionEngine {
	/**
	 * overwrite engine configuration.
	 * @param config
	 */
	public void setConfig(EngineConfig config);
	
	/**
	 * set script argument to ARGV.
	 * @param scriptArgs
	 */
	public void setArg(String[] scriptArgs);

	/**
	 * evaluate script.
	 * @param scriptName
	 * - script file name.
	 */
	public void eval(String scriptName);

	/**
	 * evaluate script from input.
	 * @param scriptName
	 * - source name.
	 * @param source
	 * - target script.
	 */
	public void eval(String scriptName, String source);

	/**
	 * evaluate one line script.
	 * @param source
	 * - target source
	 * @param lineNum
	 * - source line number.
	 */
	public void eval(String source, int lineNum);

	/**
	 * load .dshellrc file.
	 */
	public void loadDShellRC();

	public static class EngineConfig {
		private EnumSet<EngineConfigRule> ruleSet;

		public EngineConfig() {
			this.ruleSet = EnumSet.noneOf(EngineConfigRule.class);
		}

		public void enableParserInspect() {
			this.ruleSet.add(EngineConfigRule.parserInspect);
		}

		public void enableParserTrace() {
			this.ruleSet.add(EngineConfigRule.parserTrace);
		}

		public void enableByteCodeDump() {
			this.ruleSet.add(EngineConfigRule.bytecodeDump);
		}

		public boolean is(EngineConfigRule rule) {
			return this.ruleSet.contains(rule);
		}
	}

	public static enum EngineConfigRule {
		parserInspect,
		parserTrace,
		bytecodeDump;
	}
}
