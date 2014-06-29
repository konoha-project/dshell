package dshell.internal.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.commons.GeneratorAdapter;

import dshell.internal.lib.Utils;
import dshell.internal.parser.CalleeHandle;
import dshell.internal.parser.TypeUtils;
import dshell.internal.parser.CalleeHandle.ConstructorHandle;
import dshell.internal.parser.CalleeHandle.FieldHandle;
import dshell.internal.parser.CalleeHandle.MethodHandle;

class BuiltinClassType extends ClassType {
	/**
	 * used for callee handle initialization.
	 */
	protected final TypePool pool;

	protected boolean createdConstruor = false;
	/**
	 * contains constructor handle.
	 */
	protected List<ConstructorHandle> constructorHandleList;

	/**
	 * contains field handle.
	 */
	protected Map<String, HandleEntry<FieldHandle>> fieldEntryMap;

	/**
	 * contains method handle.
	 */
	protected Map<String, HandleEntry<MethodHandle>> methodEntryMap;

	protected String[][] constructorElements;
	protected String[][] fieldElements;
	protected String[][] methodElements;
	
	protected BuiltinClassType(TypePool pool, String className, 
			String internalName, DSType superType, boolean allowExtends,
			String[][] constructorElements,
			String[][] fieldElements, 
			String[][] methodElements) {
		super(className, internalName, superType, allowExtends);
		this.pool = pool;
		this.constructorElements = constructorElements;
		this.fieldElements = fieldElements;
		this.methodElements = methodElements;

		this.createdConstruor = constructorElements == null;
		if(this.fieldElements != null) {
			int size = fieldElements.length;
			this.fieldEntryMap = new HashMap<>();
			for(int i = 0; i < size; i++) {
				this.fieldEntryMap.put(fieldElements[i][0], new HandleEntry<FieldHandle>(i));
			}
		}

		if(this.methodElements != null) {
			int size = methodElements.length;
			this.methodEntryMap = new HashMap<>();
			for(int i = 0; i < size; i++) {
				this.methodEntryMap.put(methodElements[i][0], new HandleEntry<MethodHandle>(i));
			}
		}
	}

	@Override
	public ConstructorHandle lookupConstructorHandle(List<DSType> paramTypeList) {
		if(!this.createdConstruor) {
			this.createdConstruor = true;
			this.constructorHandleList = new ArrayList<>();
			for(String[] element : this.constructorElements) {
				this.constructorHandleList.add(this.toConstructorHandle(element));
			}
			this.constructorElements = null;
		}
		if(this.constructorHandleList == null) {
			return null;
		}
		for(ConstructorHandle handle : this.constructorHandleList) {
			final int size = handle.getParamTypeList().size();
			if(size != paramTypeList.size()) {
				continue;
			}
			int matchCount = 0;
			for(int i = 0; i < size; i++) {
				if(!handle.getParamTypeList().get(i).isAssignableFrom(paramTypeList.get(i))) {
					break;
				}
				matchCount++;
			}
			if(matchCount == size) {
				return handle;
			}
		}
		return null;
	}

	@Override
	public FieldHandle lookupFieldHandle(String fieldName) {
		if(this.fieldEntryMap != null) {
			HandleEntry<FieldHandle> handleEntry = this.fieldEntryMap.get(fieldName);
			if(handleEntry != null) {
				FieldHandle handle = handleEntry.handle;
				if(handle == null) {
					handle = this.toFieldHandle(this.fieldElements[handleEntry.index]);
					handleEntry.handle = handle;
				}
				return handle;
			}
		}
		return this.superType.lookupFieldHandle(fieldName);
	}

	@Override
	public MethodHandle lookupMethodHandle(String methodName) {
		if(this.methodEntryMap != null) {
			HandleEntry<MethodHandle> handleEntry = this.methodEntryMap.get(methodName);
			if(handleEntry != null) {
				MethodHandle handle = handleEntry.handle;
				if(handle == null) {
					handle = this.toMethodHandle(this.methodElements[handleEntry.index]);
					handleEntry.handle = handle;
				}
				return handle;
			}
		}
		return superType.lookupMethodHandle(methodName);
	}

	protected ConstructorHandle toConstructorHandle(String[] paramTypeNames) {
		List<DSType> paramTypeList = new ArrayList<>(paramTypeNames.length);
		for(String typeName : paramTypeNames) {
			paramTypeList.add(this.pool.parseTypeName(typeName));
		}
		return new ConstructorHandle(this, paramTypeList);
	}

	protected FieldHandle toFieldHandle(String[] symbols) {
		DSType fieldType = this.pool.parseTypeName(symbols[1]);
		return new FieldHandle(symbols[0], this, fieldType);
	}

	protected MethodHandle toMethodHandle(String[] symbols) {
		DSType returnType = this.pool.parseTypeName(symbols[1]);
		int paramSize = symbols.length - 2;
		List<DSType> paramTypeList = new ArrayList<>(paramSize);
		for(int i = 0; i < paramSize; i++) {
			paramTypeList.add(this.pool.parseTypeName(symbols[i + 2]));
		}
		return new MethodHandle(symbols[0], this, returnType, paramTypeList);
	}

	/**
	 * generate builtin type.
	 * @param typeKind
	 * - 0: BuiltinClassType, 1: StringType, 2: PrimitiveArrayType, otherwise: throw exception
	 * @param pool
	 * @param className
	 * @param internalName
	 * @param superType
	 * @param allowExtends
	 * @param constructorElements
	 * @param fieldElements
	 * @param methodElements
	 * @return
	 */
	public static DSType createType(int typeKind, TypePool pool, String className, 
			String internalName, DSType superType, boolean allowExtends,
			String[][] constructorElements,
			String[][] fieldElements, 
			String[][] methodElements) {
		switch(typeKind) {
		case 0:
			return new BuiltinClassType(pool, className, internalName, 
					superType, allowExtends, constructorElements, fieldElements, methodElements);
		case 1:
			return new StringType(pool, className, internalName, superType, 
					allowExtends, constructorElements, fieldElements, methodElements);
		case 2:
			return new PrimitiveArray(pool, className, internalName, superType, 
					allowExtends, constructorElements, fieldElements, methodElements);
		default:
			Utils.fatal(1, "unsupported type kind: " + typeKind);
		}
		return null;
	}
}

class HandleEntry<T extends CalleeHandle> {
	public final int index;
	public T handle;

	HandleEntry(int index) {
		this.index = index;
	}
}

class WrapperType extends DSType {
	WrapperType(String typeName, String internalName) {
		super(typeName, internalName);
	}
}

class WrapperMethodHandle extends MethodHandle {
	private final DSType actualOwnerType;

	WrapperMethodHandle(DSType actualOwnerType, String calleeName, DSType ownerType,
			DSType returnType, List<DSType> paramTypeList) {
		super(calleeName, ownerType, returnType, paramTypeList);
		this.actualOwnerType = actualOwnerType;
	}

	@Override
	protected void initMethodDesc() {
		if(this.ownerTypeDesc == null || this.methodDesc == null) {
			this.ownerTypeDesc = TypeUtils.toTypeDescriptor(this.actualOwnerType);
			List<DSType> actualParamTypeList = new ArrayList<>();
			actualParamTypeList.add(this.ownerType);
			for(DSType paramType : this.paramTypeList) {
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

class StringType extends BuiltinClassType {
	private final DSType wrapperType;

	protected StringType(TypePool pool, String wrapperName, String internalWrapperName,
			DSType superType, boolean allowExtends,
			String[][] constructorElements, 
			String[][] fieldElements,
			String[][] methodElements) {
		super(pool, "String", "java/lang/String", superType, allowExtends,
				constructorElements, fieldElements, methodElements);
		this.wrapperType = new WrapperType(wrapperName, internalWrapperName);
	}

	@Override
	protected MethodHandle toMethodHandle(String[] symbols) {
		DSType returnType = this.pool.parseTypeName(symbols[1]);
		int paramSize = symbols.length - 2;
		List<DSType> paramTypeList = new ArrayList<>(paramSize);
		for(int i = 0; i < paramSize; i++) {
			paramTypeList.add(this.pool.parseTypeName(symbols[i + 2]));
		}
		return new WrapperMethodHandle(this.wrapperType, symbols[0], this, returnType, paramTypeList);
	}
}

class PrimitiveArray extends BuiltinClassType implements GenericType {
	private List<DSType> elementTypeList;

	protected PrimitiveArray(TypePool pool, String className,
			String internalName, DSType superType, boolean allowExtends,
			String[][] constructorElements, String[][] fieldElements,
			String[][] methodElements) {
		super(pool, createTypeName(className), internalName, superType, allowExtends,
				constructorElements, fieldElements, methodElements);
		this.elementTypeList = new ArrayList<>(1);
		this.elementTypeList.add(this.resolveElementType(className));
		this.elementTypeList = Collections.unmodifiableList(this.elementTypeList);
	}

	private static String createTypeName(String className) {
		int index = className.indexOf("Array");
		String elementTypeName = className.substring(0, index).toLowerCase();
		return "Array<" + elementTypeName + ">";
	}

	private PrimitiveType resolveElementType(String className) {
		int index = className.indexOf("Array");
		String elementTypeName = className.substring(0, index).toLowerCase();
		return this.pool.getPrimitiveType(elementTypeName);
	}

	@Override
	public List<DSType> getElementTypeList() {
		return this.elementTypeList;
	}
}