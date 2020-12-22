package com.gentics.mesh.parameter;

import org.apache.commons.lang.BooleanUtils;

/**
 * Interface for publish query parameters.
 */
public interface PublishParameters extends ParameterProvider {

	public static final String RECURSIVE_PARAMETER_KEY = "recursive";

	/**
	 * Set the recursive flag which can be used to recursively publish a subtree of nodes.
	 * 
	 * @param flag
	 * @return Fluent API
	 */
	default PublishParameters setRecursive(boolean flag) {
		setParameter(RECURSIVE_PARAMETER_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Check whether the recursive flag for recursively publishing is enabled.
	 * 
	 * @return
	 */

	default boolean isRecursive() {
		return BooleanUtils.toBooleanDefaultIfNull(Boolean.valueOf(getParameter(RECURSIVE_PARAMETER_KEY)), false);
	}

}
