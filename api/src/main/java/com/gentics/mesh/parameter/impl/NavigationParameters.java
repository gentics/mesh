package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.NumberUtils;

public class NavigationParameters extends AbstractParameters {

	public static final String MAX_DEPTH_QUERY_PARAM_KEY = "maxDepth";

	public static final String INCLUDE_ALL_QUERY_PARAM_KEY = "includeAll";

	private Integer maxDepth;

	private Boolean includeAll;

	public NavigationParameters(ActionContext ac) {
		super(ac);
	}

	public NavigationParameters() {

	}

	/**
	 * Return the navigation maximum depth.
	 * 
	 * @return
	 */
	public Integer getMaxDepth() {
		return maxDepth;
	}

	/**
	 * Set the navigation maximum depth.
	 * 
	 * @param maxDepth
	 * @return
	 */
	public NavigationParameters setMaxDepth(Integer maxDepth) {
		this.maxDepth = maxDepth;
		return this;
	}

	/**
	 * Return the flag which indicates whether all elements should be included in the navigation.
	 * 
	 * @return
	 */
	public boolean isIncludeAll() {
		return includeAll;
	}

	/**
	 * Set the include all flag. By default only containers are included in a navigation response. When set to true all node types will be included in the
	 * navigation response.
	 * 
	 * @param flag
	 * @return
	 */
	public NavigationParameters setIncludeAll(boolean flag) {
		this.includeAll = flag;
		return this;
	}

	@Override
	protected Map<String, Object> getParameters() {
		Map<String, Object> map = new HashMap<>();
		map.put(MAX_DEPTH_QUERY_PARAM_KEY, maxDepth);
		map.put(INCLUDE_ALL_QUERY_PARAM_KEY, includeAll);
		return map;
	}

	@Override
	protected void constructFrom(ActionContext ac) {
		setMaxDepth(NumberUtils.toInteger(ac.getParameter(MAX_DEPTH_QUERY_PARAM_KEY), Mesh.mesh().getOptions().getDefaultMaxDepth()));
		setIncludeAll(Boolean.parseBoolean(ac.getParameter(INCLUDE_ALL_QUERY_PARAM_KEY)));
	}

}
