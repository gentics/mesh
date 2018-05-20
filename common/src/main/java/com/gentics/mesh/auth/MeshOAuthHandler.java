package com.gentics.mesh.auth;

import io.vertx.ext.web.RoutingContext;

public interface MeshOAuthHandler {

	void handle(RoutingContext rc);

}
