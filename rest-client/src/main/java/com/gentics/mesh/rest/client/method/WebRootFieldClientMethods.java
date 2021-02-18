package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;

public interface WebRootFieldClientMethods {

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName
	 *            name of the project
	 * @param fieldName
	 *            node field name to look
	 * @param path
	 *            requested path. Path segments must be URL encoded
	 * @param parameters
	 *            optional request parameters
	 * @return request which can return the WebRootResponse
	 */
	MeshRequest<MeshWebrootFieldResponse> webrootField(String projectName, String fieldName, String path, ParameterProvider... parameters);

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName
	 *            name of the project
	 * @param fieldName
	 *            node field name to look
	 * @param pathSegments
	 *            path segments (not URL encoded)
	 * @param parameters
	 *            optional request parameters
	 * @return request which can return the WebRootResponse
	 */
	MeshRequest<MeshWebrootFieldResponse> webrootField(String projectName, String fieldName, String[] pathSegments, ParameterProvider... parameters);
}
