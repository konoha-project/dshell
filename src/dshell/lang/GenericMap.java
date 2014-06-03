package dshell.lang;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import dshell.annotation.GenericClass;
import dshell.annotation.MapOp;
import dshell.annotation.Shared;
import dshell.annotation.SharedClass;
import dshell.annotation.TypeParameter;
import dshell.annotation.MapOp.MapOpType;
import dshell.internal.lib.Utils;

/**
 * Generic map for all of values.
 * Primitive types (long, double, boolean) are boxed.
 * @author skgchxngsxyz-osx
 *
 */
@SharedClass
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

	@Shared
	public long size() {
		return this.valueMap.size();
	}

	@Shared
	@MapOp(value = MapOpType.Getter)
	@TypeParameter()
	public Object get(String key) {
		return throwIfValueIsNull(key, this.valueMap.get(key));
	}

	@Shared
	@MapOp(value = MapOpType.Setter)
	public void set(String key, @TypeParameter() Object value) {
		this.valueMap.put(key, value);
	}

	@Shared
	public boolean hasKey(String key) {
		return this.valueMap.containsKey(key);
	}

	@Shared
	public Object remove(String key) {
		return throwIfValueIsNull(key, this.valueMap.remove(key));
	}

	@Shared
	public boolean isEmpty() {
		return this.valueMap.isEmpty();
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
