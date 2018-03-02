package com.gentics.mesh.etc.config.env;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.ANNOTATION_TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EnvironmentVariable {

	/**
	 * Description of the variable.
	 * 
	 * @return
	 */
	String description();

	/**
	 * Name of the variable.
	 * 
	 * @return
	 */
	String name();
}
