package com.gentics.mesh.plugin.forms.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link FormElement} subclass and declares which JSON type name(s) it handles.
 * The deserializer discovers annotated classes automatically via classpath scanning.
 *
 * <p>Example — single type:
 * <pre>
 *   &#64;FormElementType("boolean")
 *   public class BooleanElement extends FormElement { ... }
 * </pre>
 *
 * <p>Example — multiple types for the same class:
 * <pre>
 *   &#64;FormElementType({"c1", "c2"})
 *   public class CustomElement extends FormElement { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FormElementType {
	/**
	 * One or more JSON type name(s) that map to the annotated class.
	 */
	String[] value();
}
