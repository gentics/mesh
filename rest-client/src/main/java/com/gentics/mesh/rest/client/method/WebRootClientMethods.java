package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshWebrootResponse;

/**
 * Rest Client methods for handling webroot requests.
 */
public interface WebRootClientMethods {

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName
	 *            name of the project
	 * @param path
	 *            requested path. Path segments must be URL encoded
	 * @param parameters
	 *            optional request parameters
	 * @return request which can return the WebRootResponse
	 */
	MeshRequest<MeshWebrootResponse> webroot(String projectName, String path, ParameterProvider... parameters);

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName
	 *            name of the project
	 * @param pathSegments
	 *            path segments (not URL encoded)
	 * @param parameters
	 *            optional request parameters
	 * @return request which can return the WebRootResponse
	 */
	MeshRequest<MeshWebrootResponse> webroot(String projectName, String[] pathSegments, ParameterProvider... parameters);

	/**
	 * Invoke an update for the webroot path.
	 * 
	 * @param projectName
	 *            name of the project
	 * @param path
	 *            requested path. Path segments must be URL encoded
	 * @param nodeUpdateRequest
	 *            Update Request
	 * @param parameters
	 *            optional request parameters
	 * @return request which can return the NodeResponse
	 */
	MeshRequest<NodeResponse> webrootUpdate(String projectName, String path, NodeUpdateRequest nodeUpdateRequest, ParameterProvider... parameters);

	/**
	 * Invoke an update for the webroot path.
	 * 
	 * @param projectName
	 *            name of the project
	 * @param pathSegments
	 *            path segments (not URL encoded)
	 * @param parameters
	 *            optional request parameters
	 * @param nodeUpdateRequest
	 *            Update Request
	 * @return request which can return the NodeResponse
	 */
	MeshRequest<NodeResponse> webrootUpdate(String projectName, String[] pathSegments, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters);

	/**
	 * Create a node using the webroot path.
	 * 
	 * @param projectName
	 *            name of the project
	 * @param path
	 *            requested path. Path segments must be URL encoded
	 * @param nodeCreateRequest
	 *            Create Request
	 * @param parameters
	 *            optional request parameters
	 * @return request which can return the NodeResponse
	 */
	MeshRequest<NodeResponse> webrootCreate(String projectName, String path, NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters);

	/**
	 * Create a node using the webroot path.
	 * 
	 * @param projectName
	 *            name of the project
	 * @param pathSegments
	 *            path segments (not URL encoded)
	 * @param nodeCreateRequest
	 *            Create Request
	 * @param parameters
	 *            optional request parameters
	 * @return request which can return the NodeResponse
	 */
	MeshRequest<NodeResponse> webrootCreate(String projectName, String[] pathSegments, NodeCreateRequest nodeCreateRequest,
		ParameterProvider... parameters);
}
