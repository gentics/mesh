package com.gentics.mesh.parameter;

import org.apache.commons.lang.BooleanUtils;

/**
 * Interface for delete query parameters.
 */
public interface DeleteParameters extends ParameterProvider {

	public static final String RECURSIVE_PARAMETER_KEY = "recursive";

	/**
	 * Set the recursive flag. When enabled the deletion will also effect subelements.
	 * 
	 * @param flag
	 * @return Fluent API
	 */
	default DeleteParameters setRecursive(boolean flag) {
		setParameter(RECURSIVE_PARAMETER_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Check whether the recursive flag for recursive deletion is enabled.
	 * 
	 * @return
	 */
	default boolean isRecursive() {
		return BooleanUtils.toBooleanDefaultIfNull(Boolean.valueOf(getParameter(RECURSIVE_PARAMETER_KEY)), false);
	}
}
