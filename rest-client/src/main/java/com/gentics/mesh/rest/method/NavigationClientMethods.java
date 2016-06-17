package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.parameter.ParameterProvider;

import io.vertx.core.Future;

public interface NavigationClientMethods {

	/**
	 * Load the navigation response using the given node uuid as the root element of the navigation.
	 * 
	 * @param projectName
	 *            Project name
	 * @param uuid
	 *            Root node uuid
	 * @param parameters
	 *            Additional query parameters
	 * @return
	 */
	Future<NavigationResponse> loadNavigation(String projectName, String uuid, ParameterProvider... parameters);

}
