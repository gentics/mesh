package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.node.field.image.ImageManipulationRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantsResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;

/**
 * Methods for '/api/{version}/{project}/webrootfield/{fieldName}/{path}' endpoint.
 * 
 * @author plyhun
 *
 */
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

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName
	 *            name of the project
	 * @param fieldName
	 *            node field name to look
	 * @param path
	 *            requested path. Path segments must be URL encoded
	 * @param request
	 *            the manipulation request body
	 * @param parameters
	 *            optional request parameters
	 * @return request which can return the WebRootResponse
	 */
	MeshRequest<ImageVariantsResponse> upsertWebrootFieldImageVariants(String projectName, String fieldName, String path, ImageManipulationRequest request, ParameterProvider... parameters);

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName
	 *            name of the project
	 * @param fieldName
	 *            node field name to look
	 * @param pathSegments
	 *            path segments (not URL encoded)
	 * @param request
	 *            the manipulation request body
	 * @param parameters
	 *            optional request parameters
	 * @return request which can return the WebRootResponse
	 */
	MeshRequest<ImageVariantsResponse> upsertWebrootFieldImageVariants(String projectName, String fieldName, String[] pathSegments, ImageManipulationRequest request, ParameterProvider... parameters);
}
