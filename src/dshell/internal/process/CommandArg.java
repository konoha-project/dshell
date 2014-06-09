package dshell.internal.process;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import dshell.internal.lib.Utils;

public class CommandArg implements Serializable {
	private static final long serialVersionUID = -6997096301505284658L;

	private final String value;

	private CommandArg(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public boolean eq(String symbol) {
		return this.toString().equals(symbol);
	}

	public static CommandArg createCommandArg(String value) {
		return new CommandArg(value);
	}

	public static SubstitutedArg createSubstitutedArg(String value) {
		return new SubstitutedArg(value);
	}

	public final static class SubstitutedArg extends CommandArg {
		private static final long serialVersionUID = 4538222975122334138L;

		private final List<String> valueList;
		private String stringfyValues;

		private SubstitutedArg(String value) {
			super(value);
			this.valueList = Arrays.asList(Utils.splitWithDelim(value));
		}

		@Override
		public String toString() {
			if(this.stringfyValues == null) {
				int size = this.valueList.size();
				StringBuilder sBuilder = new StringBuilder();
				for(int i = 0; i < size; i++) {
					if(i != 0) {
						sBuilder.append(" ");
					}
					sBuilder.append(this.valueList.get(i));
				}
				this.stringfyValues = sBuilder.toString();
			}
			return this.stringfyValues;
		}

		public List<String> getValueList() {
			return this.valueList;
		}
	}
}
