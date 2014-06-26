package dshell.internal.initializer;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.commons.GeneratorAdapter;

import dshell.internal.parser.TypeUtils;
import dshell.internal.parser.CalleeHandle.MethodHandle;
import dshell.internal.parser.TypePool.Type;

public abstract class ClassWrapper extends TypeInitializer {
	protected Type ownerType;

	@Override
	protected void setMethod(Type returnType, String methodName, Type... paramTypes) {
		List<Type> paramTypeList = new ArrayList<>();
		for(Type paramType : paramTypes) {
			paramTypeList.add(paramType);
		}
		WrapperMethodHandle handle = new WrapperMethodHandle(methodName, this.ownerType, this.targetType, returnType, paramTypeList);
		this.targetType.addMethodHandle(handle);
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

	private static class WrapperMethodHandle extends MethodHandle {
		protected final Type pseudoRecvType;
		protected WrapperMethodHandle(String calleeName, Type ownerType, Type recvType, Type returnType, List<Type> paramTypeList) {
			super(calleeName, ownerType, returnType, paramTypeList);
			this.pseudoRecvType = recvType;
		}

		@Override
		protected void initMethodDesc() {
			if(this.ownerTypeDesc == null || this.methodDesc == null) {
				this.ownerTypeDesc = TypeUtils.toTypeDescriptor(this.ownerType);
				List<Type> actualParamTypeList = new ArrayList<>();
				actualParamTypeList.add(this.pseudoRecvType);
				for(Type paramType : this.paramTypeList) {
					actualParamTypeList.add(paramType);
				}
				this.methodDesc = TypeUtils.toMehtodDescriptor(this.returnType, this.calleeName, actualParamTypeList);
			}
		}

		@Override
		public void call(GeneratorAdapter adapter) {
			this.initMethodDesc();
			adapter.invokeStatic(this.ownerTypeDesc, this.methodDesc);
		}
	}
}