package com.gentics.mesh.parameter;

import org.apache.commons.lang.BooleanUtils;

/**
 * Interface for consistency check parameters
 */
public interface ConsistencyCheckParameters extends ParameterProvider {
	/**
	 * Key of the "async" parameter
	 */
	public static final String ASYNC_PARAMETER_KEY = "async";

	/**
	 * Set the async flag which can be used to run the consistency check asynchronously (as job)
	 * 
	 * @param flag
	 * @return Fluent API
	 */
	default ConsistencyCheckParameters setAsync(boolean flag) {
		setParameter(ASYNC_PARAMETER_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Check whether the async flag for asynchronous consistency check is set
	 * 
	 * @return flag value
	 */
	default boolean isAsync() {
		return BooleanUtils.toBooleanDefaultIfNull(Boolean.valueOf(getParameter(ASYNC_PARAMETER_KEY)), false);
	}
}
