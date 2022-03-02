package com.gentics.mesh.test.context;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

/**
 * Annotation for test classes or test methods, which are known to create inconsistencies
 * (so no consistency check should be performed by {@link ConsistencyRule} after the test.
 */
@Retention(RUNTIME)
public @interface NoConsistencyCheck {

}
