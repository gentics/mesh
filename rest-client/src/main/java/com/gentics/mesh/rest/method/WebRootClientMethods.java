package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;

public interface WebRootClientMethods {

	/**
	 * Return the node that was found within the given path of the project.
	 * 
	 * @param projectName
	 * @param path
	 * @param parameters
	 * @return
	 */
	Future<NodeResponse> webroot(String projectName, String path, QueryParameterProvider... parameters);

}
