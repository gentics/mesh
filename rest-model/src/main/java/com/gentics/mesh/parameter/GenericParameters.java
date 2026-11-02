package com.gentics.mesh.parameter;

import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.parameter.value.FieldsSetImpl;

/**
 * Interface for generic query parameters.
 */
public interface GenericParameters extends ParameterProvider {

	/**
	 * Query parameter key: {@value #FIELDS_PARAM_KEY}
	 */
	public static final String FIELDS_PARAM_KEY = "fields";

	/**
	 * Query parameter key: {@value #ETAG_PARAM_KEY}
	 */
	public static final String ETAG_PARAM_KEY = "etag";

	/**
	 * Return the fields which should be included in the response.
	 * 
	 * @return
	 */
	default FieldsSet getFields() {
		String value = getParameter(FIELDS_PARAM_KEY);
		if (value == null || value.isEmpty()) {
			return new FieldsSetImpl();
		} else {
			return new FieldsSetImpl(value);
		}
	}

	/**
	 * Set the fields which should be included in the response.
	 * 
	 * @param fields
	 * @return
	 */
	default GenericParameters setFields(String... fields) {
		setParameter(FIELDS_PARAM_KEY, String.join(",", fields));
		return this;
	}

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
}
