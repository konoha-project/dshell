package dshell.internal.console;

/**
 * definition of console operation.
 * @author skgchxngsxyz-osx
 *
 */
public abstract class AbstractConsole {
	protected int lineNumber;

	/**
	 * get line number. line number start at 1.
	 * @return
	 */
	public int getLineNumber() {
		return this.lineNumber;
	}

	/**
	 * 
	 * @param line
	 */
	public void incrementLineNum(String line) {
		int size = line.length();
		int count = 1;
		for(int i = 0; i < size; i++) {
			char ch = line.charAt(i);
			if(ch == '\n') {
				count++;
			}
		}
		this.lineNumber += count;
	}

	/**
	 * read string from console.
	 * @return
	 * - return null, if End of IO or IO problem.
	 */
	public abstract String readLine();

	/**
	 * print prompt and read line.
	 * @param prompt
	 * @return
	 */
	protected abstract String readLine(String prompt);

	protected String readLineImpl(String prompt1, String prompt2) {
		StringBuilder lineBuilder = new StringBuilder();
		String line = this.readLine(prompt1);
		lineBuilder.append(line);
		int level = 0;
		while((level = this.checkBraceLevel(line, level)) > 0) {
			line = this.readLine(prompt2);
			lineBuilder.append('\n');
			lineBuilder.append(line);
		}
		if(level < 0) {
			if(line == null) {
				return null;
			}
			System.out.println(" .. canceled");
			return "";
		}
		return lineBuilder.toString().trim();
	}

	protected int checkBraceLevel(String text, int level) {
		if(text == null) {
			return -1;
		}
		boolean foundDoubleQuote = false;
		boolean foundSingleQuote = false;
		int size = text.length();
		for(int i = 0; i < size; i++) {
			char ch = text.charAt(i);
			if(!foundSingleQuote && !foundDoubleQuote) {
				if(ch == '{' || ch == '[' || ch == '(') {
					level++;
				}
				if(ch == '}' || ch == ']' || ch == ')') {
					level--;
				}
				if(ch == '\'') {
					foundSingleQuote = true;
				}
				if(ch == '"') {
					foundDoubleQuote = true;
				}
			}
			else {
				if(ch == '\\') {
					i++;
					continue;
				}
				if(ch == '\'') {
					foundSingleQuote = !foundSingleQuote;
				}
				if(ch == '"') {
					foundDoubleQuote = !foundDoubleQuote;
				}
			}
		}
		return level;
	}
}
