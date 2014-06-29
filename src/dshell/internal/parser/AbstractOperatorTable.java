package dshell.internal.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dshell.internal.parser.CalleeHandle.OperatorHandle;
import dshell.internal.type.DSType;

/**
 * contains binary operator handle, unary operator handle. assign operator handle.
 * @author skgchxngsxyz-osx
 *
 */
public abstract class AbstractOperatorTable {
	private final Map<String, OperatorEntry> binaryEntryMap;
	private final Map<String, OperatorEntry> unaryEntryMap;

	public AbstractOperatorTable() {
		this.binaryEntryMap = new HashMap<>(16);
		this.unaryEntryMap = new HashMap<>(5);
	}

	/**
	 * create new OperatorHandle and put it to entryMap.
	 * if entry exists, append to exist entry.
	 * @param returnType
	 * - operator return type.
	 * @param operatorSymbol
	 * - operator name, for example '+', '=='.
	 * it is key of entryMap.
	 * @param ownerName
	 * - holder class of operator. must be fully qualified name.
	 * @param internalName
	 * - method name of this operator.
	 * @param paramTypes
	 * - may be 1, 2
	 */
	public void setOperator(DSType returnType, String operatorSymbol, String ownerName, String internalName, DSType... paramTypes) {
		int size = paramTypes.length;
		assert (size > 0 && size < 3);
		List<DSType> paramTypeList = new ArrayList<>(size);
		for(DSType paramType : paramTypes) {
			paramTypeList.add(paramType);
		}
		OperatorHandle handle = new OperatorHandle(internalName, ownerName, returnType, paramTypeList);
		if(size == 1) {
			this.addToUnaryEntryMap(operatorSymbol, handle);
		} else {
			this.addToBinaryEntryMap(operatorSymbol, handle);
		}
	}

	private void addToUnaryEntryMap(String key, OperatorHandle handle) {
		OperatorEntry entry = this.unaryEntryMap.get(key);
		if(entry == null) {
			this.unaryEntryMap.put(key, new OperatorEntry(handle));
			return;
		}
		entry.appendHandle(handle);
	}

	private void addToBinaryEntryMap(String key, OperatorHandle handle) {
		OperatorEntry entry = this.binaryEntryMap.get(key);
		if(entry == null) {
			this.binaryEntryMap.put(key, new OperatorEntry(handle));
			return;
		}
		entry.appendHandle(handle);
	}

	/**
	 * get binary operator
	 * @param operatorName
	 * @param leftType
	 * @param rightType
	 * @return
	 * return null, if has no handle
	 */
	public OperatorHandle getOperatorHandle(String operatorName, DSType leftType, DSType rightType) {
		OperatorEntry entry = this.binaryEntryMap.get(operatorName);
		return entry == null ? null : entry.lookupHandle(leftType, rightType);
	}

	public OperatorHandle getOperatorHandle(String operatorName, DSType rightType) {
		OperatorEntry entry = this.unaryEntryMap.get(operatorName);
		return entry == null ? null : entry.lookupHandle(rightType);
	}
}

/**
 * contains same name operator handle
 * @author skgchxngsxyz-osx
 *
 */
class OperatorEntry {
	/**
	 * same name handle list.
	 */
	private final List<OperatorHandle> operatorList;

	OperatorEntry(OperatorHandle handle) {
		this.operatorList = new ArrayList<>();
		this.operatorList.add(handle);
	}

	void appendHandle(OperatorHandle handle) {
		this.operatorList.add(handle);
	}

	/**
	 * get handle of binary operator.
	 * @param leftType
	 * @param rightType
	 * @return
	 * - return null, if has no handle.
	 */
	OperatorHandle lookupHandle(DSType leftType, DSType rightType) {
		for(OperatorHandle handle : this.operatorList) {
			if(handle.getParamTypeList().get(0).isAssignableFrom(leftType) &&
					handle.getParamTypeList().get(1).isAssignableFrom(rightType)) {
				return handle;
			}
		}
		return null;
	}

	/**
	 * get handle of unary operator.
	 * @param rightType
	 * @return
	 * - return null, if has no handle.
	 */
	OperatorHandle lookupHandle(DSType rightType) {
		for(OperatorHandle handle : this.operatorList) {
			if(handle.getParamTypeList().get(0).isAssignableFrom(rightType)) {
				return handle;
			}
		}
		return null;
	}
}