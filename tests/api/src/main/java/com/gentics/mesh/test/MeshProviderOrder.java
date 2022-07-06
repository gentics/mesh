package com.gentics.mesh.test;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for adding an order to instances of {@link MeshOptionsProvider}, so that that automatic
 * selection of the implementation can be controlled
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface MeshProviderOrder {
	/**
	 * Order of the implementation (smaller numbers take precedence over higher numbers)
	 * @return order
	 */
	int value();
}
