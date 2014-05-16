package dshell.internal.console;

public interface AbstractConsole {
	public int getLineNumber();
	public void incrementLineNum(String line);
	public String readLine();
}
