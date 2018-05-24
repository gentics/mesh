package com.gentics.mesh.auth.handler;

import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;

public interface MeshAuthHandler extends AuthHandler {

	default void handle401(RoutingContext context) {
		context.fail(401);
	}

	/**
	 * Authorization is done internally via roles - No need to handle it here
	 * @param user
	 * @param ctx
	 */
	default void authorizeUser(User user, RoutingContext ctx) {
		ctx.next();
	}

}
