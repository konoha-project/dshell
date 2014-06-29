package dshell.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If has this annotation, call some method by specific map operator.
 * @author skgchxngsxyz-osx
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface MapOp {
	/**
	 * Represents map operator type.
	 * @return
	 * - If unset, return MapOpType.Getter
	 */
	MapOpType value() default MapOpType.Getter;

	/**
	 * Type of map specific operator. 
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static enum MapOpType {
		/**
		 * Get element by key.
		 * map[key] -> map.get(key).
		 * Key is String class and getting value is Object class. 
		 * If not contains key, throw KeyNotFoundException.
		 */
		Getter,

		/**
		 * Put key and value to map.
		 * map[key] = value -> map.set(key, value).
		 * Key is String class and value is Object class.
		 * If contains key, overwrite old value.
		 */
		Setter,
	}
}
