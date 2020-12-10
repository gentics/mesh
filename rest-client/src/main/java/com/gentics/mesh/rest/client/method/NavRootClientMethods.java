package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

/**
 * Rest Client methods for handling nav root requests.
 */
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
	MeshRequest<NavigationResponse> navroot(String projectName, String path, ParameterProvider... parameters);
}
