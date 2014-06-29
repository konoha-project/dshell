package dshell.internal.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dshell.internal.parser.CalleeHandle.ConstructorHandle;
import dshell.internal.parser.CalleeHandle.FieldHandle;
import dshell.internal.parser.CalleeHandle.MethodHandle;

public class UserDefinedClassType extends ClassType {
	protected List<ConstructorHandle> constructorHandleList;

	/**
	 * key is field name.
	 */
	protected Map<String, FieldHandle> fieldHandleMap;

	/**
	 * key is method name.
	 */
	protected Map<String, MethodHandle> methodHandleMap;

	/**
	 * true after called finalizeType.
	 */
	protected boolean finalized = false;

	public UserDefinedClassType(String className, String internalName, DSType superType, boolean allowExtends) {
		super(className, internalName, superType, allowExtends);
	}

	public DSType getSuperType() {
		return this.superType;
	}

	@Override
	public ConstructorHandle lookupConstructorHandle(List<DSType> paramTypeList) {
		if(this.constructorHandleList != null) {
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
		}
		return null;
	}

	@Override
	public FieldHandle lookupFieldHandle(String fieldName) {
		if(this.fieldHandleMap != null) {
			FieldHandle handle = this.fieldHandleMap.get(fieldName);
			if(handle == null) {
				return this.superType.lookupFieldHandle(fieldName);
			}
			return handle;
		}
		return null;
	}

	@Override
	public MethodHandle lookupMethodHandle(String methodName) {
		if(this.methodHandleMap != null) {
			MethodHandle handle = this.methodHandleMap.get(methodName);
			if(handle == null) {
				return this.superType.lookupMethodHandle(methodName);
			}
			return handle;
		}
		return null;
	}

	@Override
	public void finalizeType() {
		if(!this.finalized) {
			this.finalized = true;
			this.constructorHandleList = Collections.unmodifiableList(this.constructorHandleList);
			this.fieldHandleMap = Collections.unmodifiableMap(this.fieldHandleMap);
			this.methodHandleMap = Collections.unmodifiableMap(this.methodHandleMap);
		}
	}

	/**
	 * create and add new filed handle. used for type checker.
	 * @param fieldName
	 * @param fieldType
	 * @return
	 * - return false, if field has already defined.
	 */
	public boolean addNewFieldHandle(String fieldName, DSType fieldType) {
		if(this.fieldHandleMap == null) {
			this.fieldHandleMap = new HashMap<>();
		}
		if(this.fieldHandleMap.containsKey(fieldName)) {
			return false;
		}
		this.fieldHandleMap.put(fieldName, new FieldHandle(fieldName, this, fieldType));
		return true;
	}

	/**
	 * create and add new method handle. used for type checker.
	 * @param methodName
	 * - method name
	 * @param returnType
	 * @param paramTypes
	 * - if has no parameter, it is empty array
	 * @return
	 * - return false, id method has already defined.
	 */
	public boolean addNewMethodHandle(String methodName, DSType returnType, DSType[] paramTypes) {
		if(this.methodHandleMap == null) {
			this.methodHandleMap = new HashMap<>();
		}
		if(this.methodHandleMap.containsKey(methodName)) {
			return false;
		}
		List<DSType> paramTypeList = new ArrayList<>();
		for(DSType paramType : paramTypes) {
			paramTypeList.add(paramType);
		}
		this.methodHandleMap.put(methodName, new MethodHandle(methodName, this, returnType, paramTypeList));
		return true;
	}

	/**
	 * create and add new constructor handle. used for type checker.
	 * @param paramTypes
	 * - if has no parameter, it is empty array
	 * @return
	 * - return false, id method has already defined.
	 */
	public boolean addNewConstructorHandle(DSType[] paramTypes) {
		if(this.constructorHandleList == null) {
			this.constructorHandleList = new ArrayList<>();
		}
		for(ConstructorHandle handle : this.constructorHandleList) {
			List<DSType> paramTypeList = handle.getParamTypeList();
			int size = paramTypeList.size();
			if(size != paramTypes.length) {
				continue;
			}
			int count = 0;
			for(int i = 0; i < size; i++) {
				if(!paramTypeList.get(i).equals(paramTypes[i])) {
					break;
				}
				count++;
			}
			if(count == size) {
				return false;
			}
		}
		List<DSType> paramTypeList = new ArrayList<>();
		for(DSType paramType : paramTypes) {
			paramTypeList.add(paramType);
		}
		this.constructorHandleList.add(new ConstructorHandle(this, paramTypeList));
		return true;
	}
}
