package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.query.QueryParameterProvider;

import io.vertx.core.Future;

public interface NavRootClientMethods {

	/**
	 * Load a navigation response for the container node that is referenced by the given path.
	 * 
	 * @param projectName
	 *            Project name
	 * @param path
	 *            Path to root node
	 * @param parameters
	 *            Additional query parameters
	 * @return
	 */
	Future<NavigationResponse> navroot(String projectName, String path, QueryParameterProvider... parameters);
}
