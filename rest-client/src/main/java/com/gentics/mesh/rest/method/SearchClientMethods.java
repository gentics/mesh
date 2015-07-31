package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.node.NodeListResponse;

public interface SearchClientMethods {

	Future<NodeListResponse> searchNodes(String json);

}
