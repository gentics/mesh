package com.gentics.mesh.parameter;

import org.apache.commons.lang.StringUtils;

/**
 * Interface for the input/output display parameters.
 * 
 * @author plyhun
 *
 */
public interface DisplayParameters extends ParameterProvider {

	/**
	 * Query parameter key: {@value #MINIFY_PARAM_KEY}
	 */
	public static final String MINIFY_PARAM_KEY = "minify";

	/**
	 * Set nullable 'minify input/output' override flag.
	 * 
	 * @param minify
	 * @return
	 */
	public default DisplayParameters setMinify(Boolean minify) {
		if (minify != null) {
			setParameter(MINIFY_PARAM_KEY, String.valueOf(minify));
		} else {
			setParameter(MINIFY_PARAM_KEY, null);
		}
		return this;
	}

	/**
	 * Get nullable 'minify input/output' override flag.
	 * 
	 * @return
	 */
	public default Boolean getMinify() {
		String minify = getParameter(MINIFY_PARAM_KEY);
		if (StringUtils.isNotBlank(minify)) {
			return Boolean.parseBoolean(minify);
		} else {
			return null;
		}
	}
}
