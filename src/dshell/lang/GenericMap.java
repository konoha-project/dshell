package dshell.lang;

import java.util.LinkedHashMap;

import dshell.lang.annotation.Exportable;
import dshell.lang.annotation.GenericClass;
import dshell.lang.annotation.MapOp;
import dshell.lang.annotation.TypeParameter;
import dshell.lang.annotation.MapOp.MapOpType;

/**
 * Generic map for all of values.
 * Primitive types (long, double, boolean) are boxed.
 * @author skgchxngsxyz-osx
 *
 */
@GenericClass
public class GenericMap {
	private final LinkedHashMap<String, Object> valueMap;

	public GenericMap(String[] keys, Object[] values) {
		this.valueMap = new LinkedHashMap<>();
		assert keys.length == values.length;
		int size = keys.length;
		for(int i = 0; i < size; i++) {
			this.valueMap.put(keys[i], values[i]);
		}
	}

	private static Object throwIfValueIsNull(String key, Object value) {
		assert key != null;
		if(value == null) {
			throw new KeyNotFoundException("not found key: " + key);
		}
		return value;
	}

	@Exportable
	public long size() {
		return this.valueMap.size();
	}

	@Exportable
	@MapOp(value = MapOpType.Getter)
	@TypeParameter()
	public Object get(String key) {
		return throwIfValueIsNull(key, this.valueMap.get(key));
	}

	@Exportable
	@MapOp(value = MapOpType.Setter)
	public void set(String key, 
			@TypeParameter() Object value) {
		this.valueMap.put(key, value);
	}

	@Exportable
	public boolean hasKey(String key) {
		return this.valueMap.containsKey(key);
	}

	@Exportable
	public Object remove(String key) {
		return throwIfValueIsNull(key, this.valueMap.remove(key));
	}

	@Exportable
	public boolean isEmpty() {
		return this.valueMap.isEmpty();
	}
}
