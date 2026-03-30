package com.gentics.mesh.parameter;

import org.apache.commons.lang.BooleanUtils;

/**
 * Parameters, applied on an entity update.
 */
public interface UpdateParameters extends ParameterProvider {

	public static final String UPSERT_PARAMETER_KEY = "upsert";
	public static final boolean DEFAULT_UPSERT_PARAMETER_VALUE = true;

	/**
	 * Set the flag to upsert (create if not found) the value on update.
	 * 
	 * @param flag
	 * @return
	 */
	default UpdateParameters setUpsert(boolean flag) {
		setParameter(UPSERT_PARAMETER_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Check if an upsert (create if not found) is requested.
	 * 
	 * @return
	 */
	default boolean isUpsert() {
		return BooleanUtils.toBooleanDefaultIfNull(BooleanUtils.toBooleanObject(getParameter(UPSERT_PARAMETER_KEY)), DEFAULT_UPSERT_PARAMETER_VALUE);
	}
}
