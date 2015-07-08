package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;

public interface WebRootClientMethods {

	Future<NodeResponse> webroot(String projectName, String path, QueryParameterProvider... parameters);

}
