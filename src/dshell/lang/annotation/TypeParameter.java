package dshell.lang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represent type parameters of generic type.
 * @author skgchxngsxyz-osx
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface TypeParameter {
	/**
	 * 
	 * @return
	 * - Contains parameter's type names. If type parameter is unresolved, contains empty string.
	 */
	String[] value() default {""};
}
