package dshell.lang;

import dshell.annotation.GenericClass;
import dshell.annotation.Shared;
import dshell.annotation.SharedClass;
import dshell.annotation.TypeAlias;
import dshell.internal.lib.Utils;

/**
 * generic pair.
 * primitive types (long, double, boolean) are boxed
 * @author skgchxngsxyz-osx
 *
 * @param <L>
 * @param <R>
 */
@SharedClass
@GenericClass(values = {"@L", "@R"})
public class GenericPair<L,R> {
	private L left;
	private R right;

	public GenericPair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	@Shared @TypeAlias("@L")
	public L getLeft() {
		return this.left;
	}

	@Shared
	public void setLeft(@TypeAlias("@L") L left) {
		this.left = left;
	}

	@Shared @TypeAlias("@R")
	public R getRight() {
		return this.right;
	}

	@Shared
	public void setRight(@TypeAlias("@R") R right) {
		this.right = right;
	}

	@Override
	@Shared
	public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append('(');
		Utils.appendStringifiedValue(sBuilder, this.left);
		sBuilder.append(", ");
		Utils.appendStringifiedValue(sBuilder, this.right);
		sBuilder.append(')');
		return sBuilder.toString();
	}
}
