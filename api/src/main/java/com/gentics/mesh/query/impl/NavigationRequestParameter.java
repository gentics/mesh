package com.gentics.mesh.query.impl;

import java.util.Map;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.query.QueryParameterProvider;
import com.gentics.mesh.util.HttpQueryUtils;
import com.gentics.mesh.util.NumberUtils;

public class NavigationRequestParameter implements QueryParameterProvider {

	public static final String MAX_DEPTH_QUERY_PARAM_KEY = "maxDepth";

	public static final String INCLUDE_ALL_QUERY_PARAM_KEY = "includeAll";

	private Integer maxDepth;

	private Boolean includeAll;

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
	public NavigationRequestParameter setIncludeAll(boolean flag) {
		this.includeAll = flag;
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
		if (includeAll != null) {
			if (query.length() != 0) {
				query.append("&");
			}
			query.append(INCLUDE_ALL_QUERY_PARAM_KEY);
			query.append("=");
			query.append(includeAll);
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
		parameter.setIncludeAll(Boolean.parseBoolean(queryParameters.get(INCLUDE_ALL_QUERY_PARAM_KEY)));
		return parameter;
	}

}
