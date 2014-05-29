package dshell.internal.parser;

import dshell.internal.parser.TypePool.Type;

public class TypeUtils {
	public static boolean matchParamsType(final Type[] paramTypes, final Type[] givenTypes) {
		assert paramTypes != null;
		assert givenTypes != null;
		if(paramTypes.length != givenTypes.length) {
			return false;
		}
		int size = paramTypes.length;
		for(int i = 0; i < size; i++) {
			if(!paramTypes[i].isAssignableFrom(givenTypes[i])) {
				return false;
			}
		}
		return true;
	}
}
