package com.gentics.mesh.parameter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
	default Set<String> getFields() {
		String value = getParameter(FIELDS_PARAM_KEY);
		if(value==null|| value.isEmpty()) {
			return new HashSet<>();
		} else {
			return new HashSet<String>(Arrays.asList(value.split(",")));
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
