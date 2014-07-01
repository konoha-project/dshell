package dshell.lang;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import dshell.annotation.GenericClass;
import dshell.annotation.MapOp;
import dshell.annotation.Shared;
import dshell.annotation.SharedClass;
import dshell.annotation.TypeAlias;
import dshell.annotation.MapOp.MapOpType;
import dshell.internal.lib.Utils;

/**
 * Generic map for all of values.
 * Primitive types (long, double, boolean) are boxed.
 * @author skgchxngsxyz-osx
 *
 */
@SharedClass
@GenericClass(values = {"@T"})
public class GenericMap {
	private final static int defaultMapSize = 12;

	private LinkedHashMap<String, Object> valueMap;

	public GenericMap(String[] keys, Object[] values) {
		assert keys.length == values.length;
		int size = keys.length;
		this.valueMap = new LinkedHashMap<>();
		for(int i = 0; i < size; i++) {
			this.valueMap.put(keys[i], values[i]);
		}
	}

	@Shared
	public GenericMap() {
		this.valueMap = new LinkedHashMap<>();
	}

	private GenericMap(LinkedHashMap<String, Object> valueMap) {
		this.valueMap = valueMap;
	}

	private static Object throwIfValueIsNull(String key, Object value) {
		assert key != null;
		if(value == null) {
			throw new KeyNotFoundException("not found key: " + key);
		}
		return value;
	}

	@Shared
	public long size() {
		return this.valueMap.size();
	}

	@Shared @MapOp(value = MapOpType.Getter) @TypeAlias("@T")
	public Object get(String key) {
		return throwIfValueIsNull(key, this.valueMap.get(key));
	}

	@Shared @MapOp(value = MapOpType.Setter)
	public void set(String key, @TypeAlias("@T") Object value) {
		this.valueMap.put(key, value);
	}

	@Shared
	public boolean hasKey(String key) {
		return this.valueMap.containsKey(key);
	}

	@Shared
	public boolean hasValue(@TypeAlias("@T") Object value) {
		return this.valueMap.containsValue(value);
	}

	@Shared @TypeAlias("@T")
	public Object remove(String key) {
		return throwIfValueIsNull(key, this.valueMap.remove(key));
	}

	@Shared
	public boolean isEmpty() {
		return this.valueMap.isEmpty();
	}

	@Shared
	public void clear() {
		if(this.size() > defaultMapSize) {
			this.valueMap = new LinkedHashMap<>();
		} else {
			this.valueMap.clear();
		}
	}

	@Shared @TypeAlias("Array<String>")
	public GenericArray keys() {
		return new GenericArray(this.valueMap.keySet().toArray());
	}

	@SuppressWarnings("unchecked")
	@Shared @TypeAlias("Map<@T>")
	public GenericMap clone() {
		return new GenericMap((LinkedHashMap<String, Object>) this.valueMap.clone());
	}

	@Shared
	@Override
	public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		int count = 0;
		sBuilder.append("{");
		for(Entry<String, Object> entry : this.valueMap.entrySet()) {
			if(count++ > 0) {
				sBuilder.append(", ");
			}
			Utils.appendStringifiedValue(sBuilder, entry.getKey());
			sBuilder.append(" : ");
			Utils.appendStringifiedValue(sBuilder, entry.getValue());
		}
		sBuilder.append("}");
		return sBuilder.toString();
	}
}
