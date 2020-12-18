package com.gentics.mesh.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used in combination with checkstyle to annotate setters which do not require javadoc. Especially fluent setters will not automatically be
 * detected by checkstyle. You can use this annotation to ignore the checkstyle warning.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.SOURCE)
public @interface Setter {

}
