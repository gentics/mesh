package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;

public interface SearchClientMethods {

	Future<NodeListResponse> searchNodes(String json, QueryParameterProvider... parameters);

}
