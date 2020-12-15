package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

/**
 * REST client methods for navigation endpoint.
 */
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
	MeshRequest<NavigationResponse> loadNavigation(String projectName, String uuid, ParameterProvider... parameters);

}
