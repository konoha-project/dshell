package dshell.internal.parser;

import java.util.ArrayList;
import java.util.List;

import dshell.internal.parser.CalleeHandle.StaticFunctionHandle;
import dshell.internal.parser.TypePool.ClassType;
import dshell.internal.parser.TypePool.Type;

public abstract class ClassWrapper {
	protected ClassType classType;
	protected TypePool pool;
	protected Type ownerType;

	public abstract void set(ClassType classType, TypePool pool);

	protected void setStaticMethod(Type returnType, String methodName, Type... paramTypes) {
		List<Type> paramTypeList = new ArrayList<>();
		for(Type paramType : paramTypes) {
			paramTypeList.add(paramType);
		}
		StaticFunctionHandle handle = new StaticFunctionHandle(methodName, this.ownerType, returnType, paramTypeList);
		this.classType.addMethodHandle(handle);
	}

	protected void createOwnerType(String internalName) {
		int index = internalName.lastIndexOf('/');
		String typeName = internalName.substring(index + 1);
		this.ownerType = new WrapperClassType(typeName, internalName);
	}

	private static class WrapperClassType extends Type {
		protected WrapperClassType(String typeName, String internalName) {
			super(typeName, internalName);
		}
	}
}
