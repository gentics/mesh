package com.gentics.mesh.query.impl;

import java.util.Map;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.query.QueryParameterProvider;
import com.gentics.mesh.util.HttpQueryUtils;
import com.gentics.mesh.util.NumberUtils;

public class NavigationRequestParameter implements QueryParameterProvider {

	public static final String MAX_DEPTH_QUERY_PARAM_KEY = "maxDepth";

	private Integer maxDepth;

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
	public NavigationRequestParameter setMaxDepth(Integer maxDepth) {
		this.maxDepth = maxDepth;
		return this;
	}

	@Override
	public String getQueryParameters() {
		StringBuilder query = new StringBuilder();
		if (maxDepth != null) {
			query.append(MAX_DEPTH_QUERY_PARAM_KEY);
			query.append("=");
			query.append(maxDepth);
		}

		return query.toString();
	}

	/**
	 * Transform the query string into a {@link NavigationRequestParameter} object.
	 * 
	 * @param query
	 * @return
	 */
	public static NavigationRequestParameter fromQuery(String query) {
		Map<String, String> queryParameters = HttpQueryUtils.splitQuery(query);
		NavigationRequestParameter parameter = new NavigationRequestParameter();
		parameter.setMaxDepth(NumberUtils.toInteger(queryParameters.get(MAX_DEPTH_QUERY_PARAM_KEY), Mesh.mesh().getOptions().getDefaultMaxDepth()));
		return parameter;
	}

}
