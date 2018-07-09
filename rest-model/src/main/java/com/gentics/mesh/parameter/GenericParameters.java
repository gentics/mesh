package com.gentics.mesh.parameter;

import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.parameter.value.FieldsSetImpl;

public interface GenericParameters extends ParameterProvider {

	/**
	 * Query parameter key: {@value #FIELDS_PARAM_KEY}
	 */
	public static final String FIELDS_PARAM_KEY = "fields";

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

}
