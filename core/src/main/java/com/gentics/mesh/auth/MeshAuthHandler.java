package com.gentics.mesh.auth;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;

/**
 * Central authentication handler for mesh.
 */
@Singleton
public class MeshAuthHandler extends AuthHandlerImpl {

	@Inject
	public MeshAuthHandler() {
		super(null);
	}

	@Override
	public void handle(RoutingContext rc) {
		System.out.println("Handler");
	}
}
