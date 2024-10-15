package com.gentics.mesh.service;

import io.vertx.ext.web.RoutingContext;

public interface AuthenticationService {

	default int priority() { return 0; }

	boolean handle(RoutingContext rc);
}
