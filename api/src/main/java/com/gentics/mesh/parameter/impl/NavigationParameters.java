package com.gentics.mesh.parameter.impl;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.NumberUtils;

public class NavigationParameters extends AbstractParameters {

	public static final String MAX_DEPTH_QUERY_PARAM_KEY = "maxDepth";

	public static final String INCLUDE_ALL_QUERY_PARAM_KEY = "includeAll";

	public NavigationParameters(ActionContext ac) {
		super(ac);
		// TODO assert values (depth must not be negative etc)
	}

	public NavigationParameters() {
		super();
	}

	/**
	 * Return the navigation maximum depth.
	 * 
	 * @return
	 */
	public Integer getMaxDepth() {
		return NumberUtils.toInteger(getParameter(MAX_DEPTH_QUERY_PARAM_KEY), Mesh.mesh().getOptions().getDefaultMaxDepth());
	}

	/**
	 * Set the navigation maximum depth.
	 * 
	 * @param maxDepth
	 * @return
	 */
	public NavigationParameters setMaxDepth(Integer maxDepth) {
		setParameter(MAX_DEPTH_QUERY_PARAM_KEY, String.valueOf(maxDepth));
		return this;
	}

	/**
	 * Return the flag which indicates whether all elements should be included in the navigation.
	 * 
	 * @return
	 */
	public boolean isIncludeAll() {
		return Boolean.parseBoolean(getParameter(INCLUDE_ALL_QUERY_PARAM_KEY));
	}

	/**
	 * Set the include all flag. By default only containers are included in a navigation response. When set to true all node types will be included in the
	 * navigation response.
	 * 
	 * @param flag
	 * @return
	 */
	public NavigationParameters setIncludeAll(boolean flag) {
		setParameter(INCLUDE_ALL_QUERY_PARAM_KEY, String.valueOf(flag));
		return this;
	}

}
