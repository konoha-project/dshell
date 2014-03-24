package dshell.lib;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class CommandArg implements Serializable {
	private static final long serialVersionUID = -6997096301505284658L;

	private final String value;

	public CommandArg(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public boolean eq(CommandArg arg) {
		return this.toString().equals(arg.toString());
	}

	public final static class SubstitutedArg extends CommandArg {
		private static final long serialVersionUID = 4538222975122334138L;

		private final List<String> valueList;
		private String stringfyValues;

		public SubstitutedArg(String value) {
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
