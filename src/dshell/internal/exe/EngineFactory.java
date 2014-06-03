package dshell.internal.exe;

public interface EngineFactory {
	/**
	 * get ExecutionEngine.
	 * call it only once.
	 * @return
	 * - throw exception, if already called.
	 */
	public ExecutionEngine getEngine();
}
