package com.gentics.mesh.parameter;

import org.apache.commons.lang.BooleanUtils;

public interface BackupParameters extends ParameterProvider {

	public static final String CONSISTENCY_CHECK_PARAMETER_KEY = "consistencyCheck";

	/**
	 * Set the consistency check flag. When enabled a consistency check will be run before invoking the backup.
	 * 
	 * @param flag
	 * @return Fluent API
	 */
	default BackupParameters setConsistencyCheck(boolean flag) {
		setParameter(CONSISTENCY_CHECK_PARAMETER_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Check whether the consistency check flag has been set.
	 * 
	 * @return
	 */
	default boolean isConsistencyCheck() {
		return BooleanUtils.toBooleanDefaultIfNull(Boolean.valueOf(getParameter(CONSISTENCY_CHECK_PARAMETER_KEY)), false);
	}
}
