package dshell.internal.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dshell.internal.parser.CalleeHandle.ConstructorHandle;
import dshell.internal.parser.CalleeHandle.FieldHandle;
import dshell.internal.parser.CalleeHandle.MethodHandle;
import dshell.internal.parser.CalleeHandle.ReifiedConstructorHandle;
import dshell.internal.parser.CalleeHandle.ReifiedFieldHandle;
import dshell.internal.parser.CalleeHandle.ReifiedMethodHandle;

/**
 * Represents generic type (array type or map type).
 * It contains elements type.
 * @author skgchxngsxyz-osx
 *
 */
public class ReifiedType extends ClassType implements GenericType {
	/**
	 * used for callee handle initialization.
	 */
	private final TypePool pool;

	private final GenericBaseType baseType;

	/**
	 * contains reified element type.
	 */
	private final List<DSType> elementTypeList;

	protected List<ConstructorHandle> constructorHandleList;

	/**
	 * key is field name.
	 */
	protected Map<String, FieldHandle> fieldHandleMap;

	/**
	 * key is method name.
	 */
	protected Map<String, MethodHandle> methodHandleMap;

	ReifiedType(TypePool pool, String typeName, GenericBaseType baseType, List<DSType> elementTypeList) {
		super(typeName, baseType.getInternalName(), baseType.superType, false);	//FIXME: super type
		this.pool = pool;
		this.baseType = baseType;
		this.elementTypeList = Collections.unmodifiableList(elementTypeList);
	}

	@Override
	public List<DSType> getElementTypeList() {
		return this.elementTypeList;
	}

	@Override
	public ConstructorHandle lookupConstructorHandle(List<DSType> paramTypeList) {	//TODO: boxed type
		if(this.constructorHandleList == null) {
			List<ConstructorHandle> baseConstructorHandles = this.baseType.getConstructorHandleList();
			this.constructorHandleList = new ArrayList<>(baseConstructorHandles.size());
			for(ConstructorHandle baseHandle : baseConstructorHandles) {
				this.constructorHandleList.add(ReifiedConstructorHandle.createReifiedHandle(pool, baseHandle, this));
			}
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
		if(this.fieldHandleMap == null) {
			this.fieldHandleMap = new HashMap<>();
		}
		FieldHandle handle = this.fieldHandleMap.get(fieldName);
		if(handle == null) {
			FieldHandle baseHandle = this.baseType.lookupFieldHandle(fieldName);
			if(baseHandle != null) {
				handle = ReifiedFieldHandle.createReifiedHandle(this.pool, baseHandle, this);
				this.fieldHandleMap.put(fieldName, handle);
			}
		}
		return handle;
	}

	@Override
	public MethodHandle lookupMethodHandle(String methodName) {
		if(this.methodHandleMap == null) {
			this.methodHandleMap = new HashMap<>();
		}
		MethodHandle handle = this.methodHandleMap.get(methodName);
		if(handle == null) {
			MethodHandle baseHandle = this.baseType.lookupMethodHandle(methodName);
			if(baseHandle != null) {
				handle = ReifiedMethodHandle.createReifiedHandle(this.pool, baseHandle, this);
				this.methodHandleMap.put(methodName, handle);
			}
		}
		return handle;
	}
}

