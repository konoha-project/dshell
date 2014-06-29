package dshell.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If has this annotation, call some method by specific array operator.
 * @author skgchxngsxyz-osx
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface ArrayOp {
	/**
	 * Represents array operator type.
	 * @return
	 * - If unset, return ArrayOpType.Getter.
	 */
	ArrayOpType value() default ArrayOpType.Getter;

	/**
	 * Type of array specific operator.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static enum ArrayOpType {
		/**
		 * Get element by index.
		 * array[index] -> array.get(index).
		 * If index out of range, throw OutOfIndexException.
		 */
		Getter,

		/**
		 * Replace old element by index to new element.
		 * array[index] = value -> array.set(index, value).
		 * If idnex out of range, throw OutOfIndexException.
		 */
		Setter,

		//TOOD: slice op may be supported.
		Slice,
	}
}
