package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.query.QueryParameterProvider;

import io.vertx.core.Future;

public interface WebRootClientMethods {

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName name of the project
	 * @param path requested path. Path segments must be URL encoded
	 * @param parameters optional request parameters
	 * @return future for the WebRootResponse
	 */
	Future<WebRootResponse> webroot(String projectName, String path, QueryParameterProvider... parameters);

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName name of the project
	 * @param pathSegments path segments (not URL encoded)
	 * @param parameters optional request parameters
	 * @return future for the WebRootResponse
	 */
	Future<WebRootResponse> webroot(String projectName, String[] pathSegments, QueryParameterProvider...parameters);
}
