package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.MeshRequest;

public interface WebRootClientMethods {

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName name of the project
	 * @param path requested path. Path segments must be URL encoded
	 * @param parameters optional request parameters
	 * @return request which can return the WebRootResponse
	 */
	MeshRequest<WebRootResponse> webroot(String projectName, String path, ParameterProvider... parameters);

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName name of the project
	 * @param pathSegments path segments (not URL encoded)
	 * @param parameters optional request parameters
	 * @return request which can return the WebRootResponse
	 */
	MeshRequest<WebRootResponse> webroot(String projectName, String[] pathSegments, ParameterProvider...parameters);
}
