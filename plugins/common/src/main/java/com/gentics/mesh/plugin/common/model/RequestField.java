package com.gentics.mesh.plugin.common.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for automatically creating a {@link com.gentics.mesh.core.rest.node.FieldMap}
 * from the annotated fields of a POJO.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestField {

	/**
	 * Indicates whether the field should be mapped to an {@link com.gentics.mesh.core.rest.node.field.HtmlField}.
	 *
	 * <p>
	 *     Only meaningful on {@link String} fields.
	 * </p>
	 *
	 * @return Whether the field is an HTML field
	 */
	boolean html() default false;

	/**
	 * Indicates whether the field should be mapped to an {@link com.gentics.mesh.core.rest.node.field.DateField}.
	 *
	 * <p>
	 *     Only meaningful on {@link String} fields.
	 * </p>
	 *
	 * @return Whether the field is a date field
	 */
	boolean date() default false;
}
