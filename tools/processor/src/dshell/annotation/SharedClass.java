package dshell.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * represent D-Shell class.
 * @author skgchxngsxyz-osx
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface SharedClass {
	/**
	 * containa super class name(not internal name).
	 * @return
	 * if has no super class, return empty string.
	 */
	String value() default "";
}
