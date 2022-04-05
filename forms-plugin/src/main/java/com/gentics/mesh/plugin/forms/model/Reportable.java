package com.gentics.mesh.plugin.forms.model;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for Implementations of {@link FormElement} for reportable fields
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Reportable {
}
