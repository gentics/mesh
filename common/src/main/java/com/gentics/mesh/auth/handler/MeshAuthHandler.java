package com.gentics.mesh.auth.handler;

import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;

/**
 * Common interface all all custom Gentics Mesh Auth handlers.
 */
public interface MeshAuthHandler extends AuthHandler {

	/**
	 * Finish the request with code 401.
	 * 
	 * @param context
	 */
	default void handle401(RoutingContext context) {
		context.fail(401);
	}

	/**
	 * Authorization is done internally via roles - No need to handle it here
	 * 
	 * @param user
	 * @param ctx
	 */
	default void authorizeUser(User user, RoutingContext ctx) {
		ctx.next();
	}

}
