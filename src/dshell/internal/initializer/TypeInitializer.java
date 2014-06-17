package dshell.internal.initializer;

import dshell.internal.parser.TypePool;
import dshell.internal.parser.TypePool.ClassType;
import dshell.internal.parser.TypePool.Type;

public abstract class TypeInitializer {
	protected TypePool pool;

	/**
	 * target type of initialization.
	 */
	protected Type targetType;
	
	public ClassType createType(TypePool pool) {	//TODO:
		return null;
	}

	public void initType(Type classtype, TypePool pool) {
		this.pool = pool;
		this.targetType = classtype;
		this.set();
		classtype.finalizeType();
	}

	protected abstract void set();

	protected void setMethod(Type returnType, String methodName, Type... paramTypes) {
		if(methodName.equals("<init>")) {
			this.targetType.addNewConstructorHandle(paramTypes);
		} else {
			this.targetType.addNewMethodHandle(methodName, returnType, paramTypes);
		}
	}
}
