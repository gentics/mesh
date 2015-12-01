package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.query.QueryParameterProvider;

import io.vertx.core.Future;

public interface WebRootClientMethods {

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName
	 * @param path
	 * @param parameters
	 * @return
	 */
	Future<WebRootResponse> webroot(String projectName, String path, QueryParameterProvider... parameters);

}
