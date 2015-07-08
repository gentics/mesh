package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.node.NodeResponse;

public interface WebRootClientMethods {

	Future<NodeResponse> webroot(String projectName, String path);

}
