package com.gentics.mesh.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used in combination with checkstyle to annotate getters which do not require javadoc. You can use this annotation to ignore the otherwise
 * created checkstyle warning.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.SOURCE)
public @interface Getter {

}
