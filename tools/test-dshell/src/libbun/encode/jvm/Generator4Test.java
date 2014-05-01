package libbun.encode.jvm;

public class Generator4Test extends DShellByteCodeGenerator {
	@Override
	public void evalAndPrint() {
		while(!this.topLevelSymbolList.isEmpty()) {
			String Symbol = this.topLevelSymbolList.remove();
			if(!this.evalAndPrintEachNode(Symbol)) {
				this.topLevelSymbolList.clear();
				System.exit(1);
			}
		}
	}

	@Override
	public boolean loadLine(String line, int lineNumber, boolean isInteractive) {
		boolean result = super.loadLine(line, lineNumber, isInteractive);
		if(isInteractive && !result) {
			System.exit(1);
		}
		return result;
	}
}
