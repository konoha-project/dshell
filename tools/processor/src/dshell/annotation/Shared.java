package dshell.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is just a marker annotation.
 * It represent shared method or field from dshell.
 * @author skgchxngsxyz-osx
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Shared {

}
