package com.gentics.mesh.parameter;

import org.apache.commons.lang.BooleanUtils;

public interface GenericParameters extends ParameterProvider {

	/**
	 * Query parameter key: {@value #OMIT_RESPONSE_PARAM_KEY}
	 */
	public static final String OMIT_RESPONSE_PARAM_KEY = "omitResponse";

	/**
	 * Return the flag which indicates whether the response should be omitted.
	 * 
	 * @return
	 */
	default boolean getOmitResponse() {
		String value = getParameter(OMIT_RESPONSE_PARAM_KEY);
		return BooleanUtils.toBooleanDefaultIfNull(BooleanUtils.toBooleanObject(value), false);
	}

	/**
	 * Set the flag which is used omit the response.
	 * 
	 * @param flag
	 * @return
	 */
	default GenericParameters setOmitResponse(boolean flag) {
		setParameter(OMIT_RESPONSE_PARAM_KEY, String.valueOf(flag));
		return this;
	}

}
