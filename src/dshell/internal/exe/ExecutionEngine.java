package dshell.internal.exe;

/**
 * definition of ExecutionEngine.
 * if you vreate your own engine, you must implement it.
 * @author skgchxngsxyz-osx
 *
 */
public interface ExecutionEngine {
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
}
