package dshell.internal.type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dshell.internal.parser.CalleeHandle.ConstructorHandle;

/**
 * Represent base type of generic type.
 * @author skgchxngsxyz-osx
 *
 */
public class GenericBaseType extends BuiltinClassType {
	private final Map<String, Integer> typeMap;

	GenericBaseType(TypePool pool, String className, 
			String internalName, DSType superType, boolean allowExtends,
			String[][] constructorElements,
			String[][] fieldElements, 
			String[][] methodElements, String optionalArg) {
		super(pool, className.substring("Generic".length()), internalName, superType, allowExtends, 
				constructorElements, fieldElements, methodElements);
		this.typeMap = new HashMap<>();
		String[] paramTypeNames = optionalArg.split(" ");
		for(int i = 0; i < paramTypeNames.length; i++) {
			this.typeMap.put(paramTypeNames[i], i);
		}
	}

	public Map<String, Integer> getTypeMap() {
		return this.typeMap;
	}

	public List<ConstructorHandle> getConstructorHandleList() {
		this.initConstructors();
		return this.constructorHandleList;
	}

	public int getElementSize() {
		return this.typeMap.size();
	}
}
