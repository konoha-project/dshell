package dshell.internal.console;

/**
 * definition of console operation.
 * @author skgchxngsxyz-osx
 *
 */
public interface AbstractConsole {
	/**
	 * get line number. line number start at 1.
	 * @return
	 */
	public int getLineNumber();

	/**
	 * 
	 * @param line
	 */
	public void incrementLineNum(String line);

	/**
	 * read string from console.
	 * @return
	 */
	public String readLine();
}
