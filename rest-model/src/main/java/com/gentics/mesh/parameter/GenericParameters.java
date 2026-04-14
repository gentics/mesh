package com.gentics.mesh.parameter;

/**
 * Interface for generic query parameters.
 */
public interface GenericParameters extends FieldsParameters {


	/**
	 * Query parameter key: {@value #ETAG_PARAM_KEY}
	 */
	public static final String ETAG_PARAM_KEY = "etag";

	/**
	 * Return whether the etag should be omitted or included.
	 * 
	 * @return
	 */
	default boolean getETag() {
		String value = getParameter(ETAG_PARAM_KEY);
		if (value != null) {
			return Boolean.valueOf(value);
		} else {
			return true;
		}
	}

	/**
	 * Set the etag inclusion flag.
	 * 
	 * @param includeEtag
	 */
	default GenericParameters setETag(boolean includeEtag) {
		setParameter(ETAG_PARAM_KEY, String.valueOf(includeEtag));
		return this;
	}

	@Override
	default GenericParameters setFields(String... fields) {
		FieldsParameters.super.setFields(fields);
		return this;
	}
}
