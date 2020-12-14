package com.gentics.mesh.parameter;

import com.gentics.mesh.util.NumberUtils;

/**
 * REST query parameters which are related to the navigation endpoint.
 */
public interface NavigationParameters extends ParameterProvider {

	public static final String MAX_DEPTH_QUERY_PARAM_KEY = "maxDepth";

	public static final String INCLUDE_ALL_QUERY_PARAM_KEY = "includeAll";

	/**
	 * Return the navigation maximum depth.
	 * 
	 * @return
	 */
	default Integer getMaxDepth() {
		return NumberUtils.toInteger(getParameter(MAX_DEPTH_QUERY_PARAM_KEY), 10);
	}

	/**
	 * Set the navigation maximum depth.
	 * 
	 * @param maxDepth
	 * @return
	 */
	default NavigationParameters setMaxDepth(Integer maxDepth) {
		setParameter(MAX_DEPTH_QUERY_PARAM_KEY, String.valueOf(maxDepth));
		return this;
	}

	/**
	 * Return the flag which indicates whether all elements should be included in the navigation.
	 * 
	 * @return
	 */
	default boolean isIncludeAll() {
		return Boolean.parseBoolean(getParameter(INCLUDE_ALL_QUERY_PARAM_KEY));
	}

	/**
	 * Set the include all flag. By default only containers are included in a navigation response. When set to true all node types will be included in the
	 * navigation response.
	 * 
	 * @param flag
	 * @return
	 */
	default NavigationParameters setIncludeAll(boolean flag) {
		setParameter(INCLUDE_ALL_QUERY_PARAM_KEY, String.valueOf(flag));
		return this;
	}
}
